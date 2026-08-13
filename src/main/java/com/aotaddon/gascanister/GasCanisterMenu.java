package com.aotaddon.gascanister;

import com.aotaddon.gas.GasCanisterItemReflection;
import com.aotaddon.gear.ModMenuTypes;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Single canister-input slot + full player inventory, mirroring GearPouchMenu's
 * layout style. Gas display comes from ContainerData (synced automatically),
 * same pattern daot's own StrwsMenu uses.
 */
public class GasCanisterMenu extends AbstractContainerMenu {

    private final GasCanisterBlockEntity blockEntity;
    private final ContainerData data;

    private static final int GUI_LEFT = 8;
    private static final int SLOT_SIZE = 18;
    private static final int CANISTER_SLOT_X = 80;
    private static final int CANISTER_SLOT_Y = 35;
    private static final int INV_TOP = 84;

    public GasCanisterMenu(int windowId, Inventory playerInventory, GasCanisterBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.GAS_CANISTER.get(), windowId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity, 0, CANISTER_SLOT_X, CANISTER_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GasCanisterItemReflection.isGasCanisterItem(stack.getItem());
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        GUI_LEFT + col * SLOT_SIZE, INV_TOP + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, GUI_LEFT + col * SLOT_SIZE, INV_TOP + 58));
        }

        addDataSlots(data);
    }

    // Note: the client-side reconstruction happens via the MenuType factory registered
    // in ModMenuTypes, which reads the BlockPos out of the opening packet and looks up
    // the real GasCanisterBlockEntity - there's no meaningful "no block" client constructor
    // for a block-backed menu like this, unlike GearPouchMenu's capability-backed one.

    public int getStoredGas() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (GasCanisterItemReflection.isGasCanisterItem(stack.getItem())) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }
}