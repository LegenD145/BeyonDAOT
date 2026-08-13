package com.aotaddon.gear;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Holds a player's ODM gear pouch: 8 blade slots, 1 gas slot, 2 spear slots.
 * Stored in player persistentData under key "GearPouch".
 * Lost on death (never copied to respawn).
 */
public class GearPouchInventory extends SimpleContainer {

    public static final int BLADE_SLOTS  = 8;
    public static final int GAS_SLOTS    = 1;
    public static final int SPEAR_SLOTS  = 2;
    public static final int TOTAL_SLOTS  = BLADE_SLOTS + GAS_SLOTS + SPEAR_SLOTS; // 11

    // Slot index ranges
    public static final int BLADE_START  = 0;
    public static final int BLADE_END    = 7;   // inclusive
    public static final int GAS_SLOT     = 8;
    public static final int SPEAR_START  = 9;
    public static final int SPEAR_END    = 10;  // inclusive

    private static final String NBT_KEY = "GearPouch";

    public GearPouchInventory() {
        super(TOTAL_SLOTS);
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    public void save(Player player, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", this.createTag(registries));
        player.getPersistentData().put(NBT_KEY, tag);
    }

    public void load(Player player, HolderLookup.Provider registries) {
        CompoundTag root = player.getPersistentData().getCompound(NBT_KEY);
        if (root.contains("Items")) {
            this.fromTag(root.getList("Items", 10), registries);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Count blade component items across blade slots. */
    public int countBladeComponents() {
        int total = 0;
        for (int i = BLADE_START; i <= BLADE_END; i++) {
            ItemStack s = getItem(i);
            if (!s.isEmpty()) total += s.getCount();
        }
        return total;
    }

    /** Consume a set number of blade components from blade slots. */
    public void consumeBladeComponents(int amount) {
        int remaining = amount;
        for (int i = BLADE_START; i <= BLADE_END && remaining > 0; i++) {
            ItemStack s = getItem(i);
            if (s.isEmpty()) continue;
            int remove = Math.min(remaining, s.getCount());
            s.shrink(remove);
            remaining -= remove;
        }
    }

    /** True if this slot index is a blade slot. */
    public static boolean isBladeSlot(int slot) {
        return slot >= BLADE_START && slot <= BLADE_END;
    }

    /** True if this slot index is the gas slot. */
    public static boolean isGasSlot(int slot) {
        return slot == GAS_SLOT;
    }

    /** True if this slot index is a spear slot. */
    public static boolean isSpearSlot(int slot) {
        return slot >= SPEAR_START && slot <= SPEAR_END;
    }
}