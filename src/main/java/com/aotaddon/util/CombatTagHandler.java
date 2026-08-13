package com.aotaddon.util;

import com.aotaddon.combat.CombatTagData;
import com.aotaddon.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class CombatTagHandler {

    private static final long TAG_DURATION_TICKS = 1600L; // 80s * 20 ticks/sec

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return; // only real, post-mitigation damage tags

        CombatTagData data = player.getData(ModAttachments.COMBAT_TAG);
        data.combatExpiryTick = player.level().getGameTime() + TAG_DURATION_TICKS;
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
                    data.combatExpiryTick = -1; // mark cleared so we don't repeat the message
                }
                continue;
            }

            if (currentTick % 20 == 0) {
                long secondsLeft = (data.combatExpiryTick - currentTick) / 20;
                player.displayClientMessage(
                        Component.literal("Combat Tagged " + secondsLeft).withStyle(ChatFormatting.RED),
                        true);
            }
        }
    }

    public static boolean isInCombat(ServerPlayer player) {
        CombatTagData data = player.getData(ModAttachments.COMBAT_TAG);
        return data.combatExpiryTick > player.level().getGameTime();
    }
}