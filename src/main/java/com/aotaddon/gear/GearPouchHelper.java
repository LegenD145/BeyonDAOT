package com.aotaddon.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Central helper — gets or saves a player's GearPouchInventory.
 * Stored in player persistentData; survives respawn via PlayerCloneDataHandler.
 */
public class GearPouchHelper {

    /**
     * Load a fresh GearPouchInventory from the player's persistentData.
     * Always call this right before opening the menu to get current state.
     */
    public static GearPouchInventory getPouch(Player player) {
        GearPouchInventory pouch = new GearPouchInventory();
        pouch.load(player, player.level().registryAccess());
        return pouch;
    }

    public static void savePouch(Player player, GearPouchInventory pouch) {
        pouch.save(player, player.level().registryAccess());
    }

    /**
     * Count blade components available in the pouch.
     * Used by the blade reload Mixin.
     */
    public static int countBladeComponents(Player player) {
        GearPouchInventory pouch = getPouch(player);
        return pouch.countBladeComponents();
    }

    /**
     * Consume blade components from the pouch and save.
     * Used by the blade reload Mixin.
     */
    public static void consumeBladeComponents(Player player, int amount) {
        GearPouchInventory pouch = getPouch(player);
        pouch.consumeBladeComponents(amount);
        savePouch(player, pouch);
    }

    /**
     * Open the gear pouch menu for a server player.
     */
    public static void openForPlayer(ServerPlayer player) {
        GearPouchInventory pouch = getPouch(player);
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (windowId, inv, p) -> new GearPouchMenu(windowId, inv, pouch),
                net.minecraft.network.chat.Component.literal("Gear Pouch")
        ));
    }
}