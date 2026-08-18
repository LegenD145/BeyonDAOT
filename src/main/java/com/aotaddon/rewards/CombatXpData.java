package com.aotaddon.rewards;

import com.aotaddon.network.PlayerCardSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Combat XP from titan kills. Key: "combatXp" — double, same persistentData
 * style as HonorData.
 */
public final class CombatXpData {

    private static final String KEY_BALANCE = "combatXp";

    private CombatXpData() {}

    public static double getBalance(Player player) {
        return player.getPersistentData().getDouble(KEY_BALANCE);
    }

    public static void addBalance(Player player, double amount) {
        player.getPersistentData().putDouble(KEY_BALANCE, getBalance(player) + amount);
    }

    public static void syncOnLogin(ServerPlayer player) {
        PlayerCardSyncPayload.send(player);
    }
}
