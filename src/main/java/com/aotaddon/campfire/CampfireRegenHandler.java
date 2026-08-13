package com.aotaddon.campfire;

import com.aotaddon.util.CombatTagHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Grants Regeneration to players standing near a lit campfire, but only
 * while they're out of combat (see CombatTagHandler).
 *
 * Checked once per second (every 20 ticks) rather than every tick, since a
 * block-radius scan every tick for every player is wasteful. The effect
 * duration (60 ticks) comfortably outlasts the 20-tick check interval, so
 * there's no visible flicker between refreshes.
 */
public class CampfireRegenHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int SEARCH_RADIUS_HORIZONTAL = 5;
    private static final int SEARCH_RADIUS_VERTICAL = 2;
    private static final int REGEN_DURATION_TICKS = 60;
    private static final int REGEN_AMPLIFIER = 0;

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CombatTagHandler.isInCombat(player)) continue;
            if (isNearLitCampfire(player)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, REGEN_DURATION_TICKS, REGEN_AMPLIFIER, true, false));
            }
        }
    }

    private static boolean isNearLitCampfire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        for (int dx = -SEARCH_RADIUS_HORIZONTAL; dx <= SEARCH_RADIUS_HORIZONTAL; dx++) {
            for (int dy = -SEARCH_RADIUS_VERTICAL; dy <= SEARCH_RADIUS_VERTICAL; dy++) {
                for (int dz = -SEARCH_RADIUS_HORIZONTAL; dz <= SEARCH_RADIUS_HORIZONTAL; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}