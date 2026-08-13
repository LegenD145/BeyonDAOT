package com.aotaddon.currency;

/**
 * Reads and writes player medal balance from persistentData.
 * Key: "medalBalance" — int, total medal value stored.
 *
 * No entity imports at class level — kept inline to avoid Transition crash.
 */
public class MedalData {

    private static final String KEY_BALANCE = "medalBalance";

    public static int getBalance(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getInt(KEY_BALANCE);
    }

    public static void setBalance(net.minecraft.world.entity.player.Player player, int amount) {
        player.getPersistentData().putInt(KEY_BALANCE, Math.max(0, amount));
    }

    public static void addBalance(net.minecraft.world.entity.player.Player player, int amount) {
        setBalance(player, getBalance(player) + amount);
    }

    public static boolean deductBalance(net.minecraft.world.entity.player.Player player, int amount) {
        int current = getBalance(player);
        if (current < amount) return false;
        setBalance(player, current - amount);
        return true;
    }
}