package com.aotaddon.gear;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the Gear Pouch — 11 slots + player hotbar.
 * Layout (top to bottom, left to right):
 *   Row 0:  blade[0..3]
 *   Row 1:  blade[4..7]
 *   Row 2:  gas[8], spear[9], spear[10]
 */
public class GearPouchMenu extends AbstractContainerMenu {

    private final GearPouchInventory pouch;
    private final Player player;

    // GUI pixel positions
    private static final int GUI_LEFT   = 8;
    private static final int BLADE_TOP  = 18;
    private static final int SLOT_SIZE  = 18;
    private static final int MISC_TOP   = BLADE_TOP + SLOT_SIZE * 2 + 4;

    // ── Server-side constructor (opened from capability) ──────────────────────
    public GearPouchMenu(int windowId, Inventory playerInv, GearPouchInventory pouch) {
        super(ModMenuTypes.GEAR_POUCH.get(), windowId);
        this.pouch  = pouch;
        this.player = playerInv.player;

        // Blade slots — 2 rows of 4
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = row * 4 + col;
                addSlot(new Slot(pouch, slotIndex,
                        GUI_LEFT + col * SLOT_SIZE,
                        BLADE_TOP + row * SLOT_SIZE));
            }
        }

        // Gas slot
        addSlot(new Slot(pouch, GearPouchInventory.GAS_SLOT,
                GUI_LEFT,
                MISC_TOP));

        // Spear slots
        addSlot(new Slot(pouch, GearPouchInventory.SPEAR_START,
                GUI_LEFT + SLOT_SIZE + 4,
                MISC_TOP));
        addSlot(new Slot(pouch, GearPouchInventory.SPEAR_END,
                GUI_LEFT + (SLOT_SIZE + 4) * 2,
                MISC_TOP));

        // Player hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col,
                    GUI_LEFT + col * SLOT_SIZE,
                    MISC_TOP + SLOT_SIZE + 14));
        }
    }

    // ── Client-side constructor (called by MenuType factory) ──────────────────
    public GearPouchMenu(int windowId, Inventory playerInv) {
        this(windowId, playerInv, GearPouchHelper.getPouch(playerInv.player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Simple shift-click: move between pouch and hotbar
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int pouchSize = GearPouchInventory.TOTAL_SLOTS;
            if (index < pouchSize) {
                // Pouch → hotbar
                if (!moveItemStackTo(stack, pouchSize, pouchSize + 9, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Hotbar → pouch
                if (!moveItemStackTo(stack, 0, pouchSize, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Save on close
        GearPouchHelper.savePouch(player);
    }
}