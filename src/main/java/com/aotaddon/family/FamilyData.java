package com.aotaddon.family;

public class FamilyData {

    private static final String KEY_FAMILY    = "family";
    private static final String KEY_KILLS     = "helosKills";
    private static final String KEY_REVIVE_TS = "reviveTimestamp";

    public static final int  HELOS_MAX_KILLS    = 5;
    public static final long REVIVE_COOLDOWN_MS = 5L * 60L * 60L * 1000L;

    // =========================================================================
    // FAMILY
    // =========================================================================

    public static String getFamily(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getString(KEY_FAMILY);
    }

    public static void setFamily(net.minecraft.world.entity.player.Player player, String family) {
        player.getPersistentData().putString(KEY_FAMILY, family.toLowerCase());
    }

    public static boolean hasFamily(net.minecraft.world.entity.player.Player player) {
        String f = getFamily(player);
        return f != null && !f.isEmpty();
    }

    public static boolean isHelos(net.minecraft.world.entity.player.Player player)  { return "helos".equals(getFamily(player));  }
    public static boolean isFritz(net.minecraft.world.entity.player.Player player)  { return "fritz".equals(getFamily(player));  }
    public static boolean isYeager(net.minecraft.world.entity.player.Player player) { return "yeager".equals(getFamily(player)); }
    public static boolean isReiss(net.minecraft.world.entity.player.Player player)  { return "reiss".equals(getFamily(player));  }

    public static boolean hasRoyalBlood(net.minecraft.world.entity.player.Player player) {
        return isFritz(player) || isReiss(player);
    }

    public static boolean hasRevive(net.minecraft.world.entity.player.Player player) {
        return isFritz(player) || isYeager(player);
    }

    // =========================================================================
    // HELOS KILL COUNTER
    // =========================================================================

    public static int getHelosKills(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getInt(KEY_KILLS);
    }

    public static void setHelosKills(net.minecraft.world.entity.player.Player player, int count) {
        player.getPersistentData().putInt(KEY_KILLS, Math.max(0, Math.min(count, HELOS_MAX_KILLS)));
    }

    public static int incrementHelosKills(net.minecraft.world.entity.player.Player player) {
        int current = getHelosKills(player);
        if (current >= HELOS_MAX_KILLS) return HELOS_MAX_KILLS;
        int next = current + 1;
        setHelosKills(player, next);
        return next;
    }

    public static void resetHelosKills(net.minecraft.world.entity.player.Player player) {
        player.getPersistentData().putInt(KEY_KILLS, 0);
    }

    public static boolean isHelosReady(net.minecraft.world.entity.player.Player player) {
        return getHelosKills(player) >= HELOS_MAX_KILLS;
    }

    public static int getStarTier(net.minecraft.world.entity.player.Player player) {
        int kills = getHelosKills(player);
        if (kills >= 4) return 3;
        if (kills >= 2) return 2;
        if (kills >= 1) return 1;
        return 0;
    }

    // =========================================================================
    // REVIVE TIMESTAMP
    // =========================================================================

    public static long getReviveTimestamp(net.minecraft.world.entity.player.Player player) {
        String raw = player.getPersistentData().getString(KEY_REVIVE_TS);
        if (raw == null || raw.isEmpty()) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static void setReviveTimestamp(net.minecraft.world.entity.player.Player player, long ts) {
        player.getPersistentData().putString(KEY_REVIVE_TS, String.valueOf(ts));
    }

    public static boolean isReviveOnCooldown(net.minecraft.world.entity.player.Player player) {
        long last = getReviveTimestamp(player);
        if (last == 0L) return false;
        return (System.currentTimeMillis() - last) < REVIVE_COOLDOWN_MS;
    }
}