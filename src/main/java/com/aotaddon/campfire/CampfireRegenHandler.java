package com.aotaddon.campfire;

import com.aotaddon.config.AddonConfig;
import com.aotaddon.util.CombatTagHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Heals a player only while they are sitting on a campfire seat.
 * Standing near a fire no longer grants vanilla Regeneration.
 *
 * Rate: empty to full max health in {@link AddonConfig#CAMPFIRE_SIT_FULL_HEAL_SECONDS}.
 */
public class CampfireRegenHandler {

    public static void onServerTick(ServerTickEvent.Post event) {
        int seconds = AddonConfig.CAMPFIRE_SIT_FULL_HEAL_SECONDS.get();
        if (seconds <= 0) {
            return;
        }

        MinecraftServer server = event.getServer();
        float healPerTick = 1.0f / (seconds * 20.0f);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.getVehicle() instanceof CampfireSeatEntity)) {
                continue;
            }
            if (CombatTagHandler.isInCombat(player)) {
                continue;
            }
            if (player.getHealth() >= player.getMaxHealth()) {
                continue;
            }
            player.heal(player.getMaxHealth() * healPerTick);
        }
    }
}
