package com.aotaddon.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Central helper — gets or saves a player's GearPouchInventory.
 * The pouch is stored in player.getPersistentData() so it is tied to the
 * player entity and wiped on death automatically (Minecraft does NOT copy
 * persistentData to the respawn player by default in NeoForge).
 *
 * If you want it to survive non-permadeath respawns, listen to
 * PlayerEvent.Clone and copy the tag manually. For now, it wipes on death.
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

    /**
     * Save a pouch back into the player's persistentData.
     * Called on menu close and after blade consumption.
     */
    public static void savePouch(Player player) {
        // The menu holds a reference to the pouch — we need the menu's pouch.
        // This is a convenience overload for when you already have the pouch.
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