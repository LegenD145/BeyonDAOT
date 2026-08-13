package com.aotaddon.rewards;

import com.aotaddon.network.HonorSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Reads and writes a player's Honor Point balance and first-kill bonus flags
 * from persistentData — same pattern as MedalData/BanknoteData in
 * com.aotaddon.currency, just a double instead of an int since Honor uses
 * sub-1 per-kill rates (0.35, 0.25, etc).
 *
 * Key: "honorPoints" — double, total Honor Points.
 * First-kill flags: "firstKillGranted_<id>" — boolean, one per bonus-eligible entity.
 */
public class HonorData {

    private static final String KEY_BALANCE = "honorPoints";
    private static final String KEY_FIRST_KILL_PREFIX = "firstKillGranted_";

    public static double getBalance(Player player) {
        return player.getPersistentData().getDouble(KEY_BALANCE);
    }

    public static void addBalance(Player player, double amount) {
        double current = getBalance(player);
        double updated = current + amount;
        player.getPersistentData().putDouble(KEY_BALANCE, updated);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new HonorSyncPayload(updated));
        }
    }

    /** Call once on player login so the client HUD isn't stuck at 0 until the next kill. */
    public static void syncOnLogin(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new HonorSyncPayload(getBalance(player)));
    }

    /**
     * Returns true and marks the flag if this is the FIRST time this bonus id
     * is being claimed (persists across relogs). Returns false if already claimed.
     * Pass a flag id, not necessarily the entity id directly — the shared
     * Abnormal/Crawling-Abnormal bonus uses one combined flag id for both.
     */
    public static boolean claimFirstKillBonus(Player player, String flagId) {
        String key = KEY_FIRST_KILL_PREFIX + flagId;
        if (player.getPersistentData().getBoolean(key)) {
            return false;
        }
        player.getPersistentData().putBoolean(key, true);
        return true;
    }

    public static boolean claimFirstKillBonus(Player player, ResourceLocation entityId) {
        return claimFirstKillBonus(player, entityId.toString());
    }
}