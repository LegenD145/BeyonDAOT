package com.aotaddon.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stores and reads the Warhammer inheritance flag on the player's
 * persistent NBT data. Survives death, server restarts, and titan deaths.
 *
 * Lost only when the Attack Titan player gets eaten (handled in EatDetectMixin).
 */
public class WarhammerInheritanceTracker {

    private static final String KEY = "aotaddon_warhammer_inherited";

    public static boolean hasInheritance(ServerPlayer player) {
        return player.getPersistentData().getBoolean(KEY);
    }

    public static void grantInheritance(ServerPlayer player) {
        if (hasInheritance(player)) return;
        player.getPersistentData().putBoolean(KEY, true);
        player.displayClientMessage(
                Component.literal("§6You have consumed the Warhammer Titan's power. Slots 5, 8 and 9 are now unlocked."),
                false
        );
    }

    public static void revokeInheritance(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
    }
}