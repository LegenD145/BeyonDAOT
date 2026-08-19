package com.aotaddon.reputation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class ReputationData {

    private static final String KEY_PARADIS = "rep_paradis";
    private static final String KEY_MARLEY = "rep_marley";
    private static final int MIN = 0;
    private static final int MAX = 100;

    public static int getParadis(Player player) {
        return player.getPersistentData().getInt(KEY_PARADIS);
    }

    public static int getMarley(Player player) {
        return player.getPersistentData().getInt(KEY_MARLEY);
    }

    public static void addParadis(Player player, int amount) {
        CompoundTag data = player.getPersistentData();
        data.putInt(KEY_PARADIS, clamp(data.getInt(KEY_PARADIS) + amount));
    }

    public static void addMarley(Player player, int amount) {
        CompoundTag data = player.getPersistentData();
        data.putInt(KEY_MARLEY, clamp(data.getInt(KEY_MARLEY) + amount));
    }

    private static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
