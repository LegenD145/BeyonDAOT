package com.aotaddon.access;

import com.aotaddon.gear.ModMenuTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PlayerInventoryAccessMenu extends AbstractContainerMenu {

    private static final EquipmentSlot[] ARMOR_SLOT_IDS = new EquipmentSlot[]{
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    public final int targetEntityId;
    private final Player targetPlayer;

    public PlayerInventoryAccessMenu(int windowId, Inventory viewerInventory, int targetEntityId) {
        // TODO: replace `null` with your registered MenuType supplier, e.g. ModMenus.PLAYER_INVENTORY_ACCESS.get()
        super(ModMenuTypes.PLAYER_INVENTORY_ACCESS.get(), windowId);

        this.targetEntityId = targetEntityId;

        Entity resolvedTarget = viewerInventory.player.level().getEntity(targetEntityId);
        this.targetPlayer = (resolvedTarget instanceof Player p) ? p : null;

        Inventory targetInv = (this.targetPlayer != null)
                ? this.targetPlayer.getInventory()
                : new Inventory(viewerInventory.player);

        for (int i = 0; i < 4; i++) {
            final EquipmentSlot equipmentSlot = ARMOR_SLOT_IDS[i];
            this.addSlot(new Slot(targetInv, 36 + (3 - i), 8, 8 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return targetPlayer != null && stack.canEquip(equipmentSlot, targetPlayer);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        this.addSlot(new Slot(targetInv, 40, 77, 62));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                this.addSlot(new Slot(targetInv, index, 8 + col * 18, 8 + row * 18 + 26));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(targetInv, col, 8 + col * 18, 8 + 3 * 18 + 26 + 4));
        }

        int viewerOffsetY = 8 + 4 * 18 + 26 + 24;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                this.addSlot(new Slot(viewerInventory, index, 8 + col * 18, viewerOffsetY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(viewerInventory, col, 8 + col * 18, viewerOffsetY + 3 * 18 + 4));
        }
    }

    @Override
    public boolean stillValid(Player viewer) {
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return false;
        }
        if (!(viewer.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return true;
        }
        boolean cuffed = DaotBridge.isCuffed(targetPlayer);
        boolean consented = ConsentManager.get(serverLevel.getServer()).isOpen(targetPlayer);
        return cuffed || consented;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        int targetSectionSize = 32;
        boolean fromTargetSide = index < targetSectionSize;

        if (fromTargetSide) {
            if (!this.moveItemStackTo(sourceStack, targetSectionSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(sourceStack, 0, targetSectionSize, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return copy;
    }
}