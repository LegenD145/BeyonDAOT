package com.aotaddon.util;

import com.aotaddon.combat.CombatTagData;
import com.aotaddon.config.AddonConfig;
import com.aotaddon.network.CombatTagSyncPayload;
import com.aotaddon.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class CombatTagHandler {

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return; // only real, post-mitigation damage tags

        int seconds = AddonConfig.COMBAT_TAG_SECONDS.get();
        if (seconds <= 0) return;

        CombatTagData data = player.getData(ModAttachments.COMBAT_TAG);
        data.combatExpiryTick = player.level().getGameTime() + (seconds * 20L);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CombatTagData data = player.getData(ModAttachments.COMBAT_TAG);
            if (data.combatExpiryTick == 0L) continue; // never tagged, skip entirely

            long currentTick = player.level().getGameTime();

            if (currentTick >= data.combatExpiryTick) {
                if (data.combatExpiryTick != -1) {
                    player.displayClientMessage(
                            Component.literal("No longer in combat").withStyle(ChatFormatting.GREEN),
                            true);
                    PacketDistributor.sendToPlayer(player, new CombatTagSyncPayload(0));
                    data.combatExpiryTick = -1;
                }
                continue;
            }

            if (currentTick % 20 == 0) {
                int secondsLeft = (int) ((data.combatExpiryTick - currentTick) / 20);
                player.displayClientMessage(
                        Component.literal("Combat Tagged " + secondsLeft).withStyle(ChatFormatting.RED),
                        true);
                PacketDistributor.sendToPlayer(player, new CombatTagSyncPayload(secondsLeft));
            }
        }
    }

    public static boolean isInCombat(Player player) {
        CombatTagData data = player.getData(ModAttachments.COMBAT_TAG);
        return data.combatExpiryTick > player.level().getGameTime();
    }
}