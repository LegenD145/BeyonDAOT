package com.aotaddon.currency;

/**
 * Marley-side mirror of MedalData. Separate persistentData key from
 * medalBalance on purpose - if a player's bloodline ever changes, their
 * old faction's balance doesn't leak into the new one.
 */
public class BanknoteData {

    private static final String KEY_BALANCE = "banknoteBalance";

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