package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Tracks which players have inherited Warhammer Titan powers
 * by eating a Warhammer shifter while in the Attack Titan.
 *
 * Stored in the player's persistent NBT data so it survives:
 * - Server restarts
 * - Titan deaths (dismounting)
 *
 * Lost only when the player themselves is eaten by a pure titan
 * or another shifter (handled in AttackTitanEatMixin).
 */
public class WarhammerInheritanceTracker {

    private static final String NBT_KEY = "aotaddon_warhammer_inherited";

    // =========================================================================
    // READ / WRITE
    // =========================================================================

    /**
     * Returns true if this player has inherited Warhammer powers.
     */
    public static boolean hasInheritance(ServerPlayer player) {
        try {
            CompoundTag persistent = player.getPersistentData();
            return persistent.getBoolean(NBT_KEY);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerInheritanceTracker.hasInheritance failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Grants Warhammer inheritance to this player.
     * Called when the Attack Titan eats a Warhammer shifter.
     */
    public static void grantInheritance(ServerPlayer player) {
        try {
            player.getPersistentData().putBoolean(NBT_KEY, true);
            AotAddon.LOGGER.info("[AotAddon] Granted Warhammer inheritance to {}",
                    player.getName().getString());
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerInheritanceTracker.grantInheritance failed: {}", e.getMessage());
        }
    }

    /**
     * Revokes Warhammer inheritance from this player.
     * Called when the player is eaten by a pure titan or another shifter.
     */
    public static void revokeInheritance(ServerPlayer player) {
        try {
            player.getPersistentData().putBoolean(NBT_KEY, false);
            AotAddon.LOGGER.info("[AotAddon] Revoked Warhammer inheritance from {}",
                    player.getName().getString());
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerInheritanceTracker.revokeInheritance failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // LOOKUP BY UUID (server-side, no player object needed)
    // =========================================================================

    /**
     * Checks inheritance by UUID using the server's player list.
     * Returns false if the player is offline or not found.
     */
    public static boolean hasInheritanceByUUID(Object serverLevel, UUID uuid) {
        try {
            // serverLevel is class_3218 (ServerLevel) — get the server from it
            Method getServerMethod = serverLevel.getClass().getMethod("getServer");
            Object server = getServerMethod.invoke(serverLevel);

            Method getPlayerMethod = server.getClass().getMethod("getPlayerList");
            Object playerList = getPlayerMethod.invoke(server);

            Method getPlayerMethod2 = playerList.getClass().getMethod("getPlayer", UUID.class);
            Object player = getPlayerMethod2.invoke(playerList, uuid);

            if (!(player instanceof ServerPlayer sp)) return false;
            return hasInheritance(sp);

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] hasInheritanceByUUID failed: {}", e.getMessage());
            return false;
        }
    }
}
