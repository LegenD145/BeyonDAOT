package com.aotaddon.rewards;

import com.aotaddon.currency.BanknoteData;
import com.aotaddon.currency.CurrencyFaction;
import com.aotaddon.currency.MedalData;
import com.aotaddon.network.CurrencySyncPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Periodically pushes each online player's currency balance (Medals or
 * Banknotes, whichever matches their faction) to their client, so the HUD
 * stays correct even when the balance changes from something other than a
 * titan kill (shop purchase, admin command, etc) — not just relying on
 * TitanKillRewardHandler's per-kill sync.
 *
 * Register via: NeoForge.EVENT_BUS.addListener(CurrencySyncTicker::onServerTick);
 */
public final class CurrencySyncTicker {

    private static final int CHECK_INTERVAL_TICKS = 20; // once per second

    private CurrencySyncTicker() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CurrencyFaction.Faction faction = CurrencyFaction.get(player);
            int balance = switch (faction) {
                case ELDIAN -> MedalData.getBalance(player);
                case MARLEY -> BanknoteData.getBalance(player);
                case NONE -> 0;
            };
            PacketDistributor.sendToPlayer(player, new CurrencySyncPayload(balance));
        }
    }
}