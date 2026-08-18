package com.aotaddon.rewards;

import com.aotaddon.currency.BanknoteData;
import com.aotaddon.currency.CurrencyFaction;
import com.aotaddon.currency.MedalData;
import com.aotaddon.network.CurrencySyncPayload;
import com.aotaddon.network.PlayerCardSyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pushes each online player's DAOT-bloodline currency (medals or banknotes)
 * to their client once per second.
 *
 * Register via: NeoForge.EVENT_BUS.addListener(CurrencySyncTicker::onServerTick);
 */
public final class CurrencySyncTicker {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private CurrencySyncTicker() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int balance = switch (CurrencyFaction.get(player)) {
                case ELDIAN -> MedalData.getBalance(player);
                case MARLEY -> BanknoteData.getBalance(player);
                case NONE -> 0;
            };
            PacketDistributor.sendToPlayer(player, new CurrencySyncPayload(balance));
            PlayerCardSyncPayload.send(player);
        }
    }
}
