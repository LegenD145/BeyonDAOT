package com.aotaddon.currency;

/**
 * Where the player lives for currency, independent of DAOT bloodline.
 * Key: "currencyResidence" — "eldia", "marley", or empty (no wallet).
 */
public final class ResidenceData {

    private static final String KEY = "currencyResidence";

    private ResidenceData() {}

    public static String get(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getString(KEY);
    }

    public static boolean hasResidence(net.minecraft.world.entity.player.Player player) {
        String residence = get(player);
        return "eldia".equals(residence) || "marley".equals(residence);
    }

    public static void set(net.minecraft.world.entity.player.Player player, String residence) {
        if (residence == null || residence.isBlank() || "clear".equalsIgnoreCase(residence)) {
            player.getPersistentData().remove(KEY);
            return;
        }
        player.getPersistentData().putString(KEY, residence.toLowerCase());
    }
}
