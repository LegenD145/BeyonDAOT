package com.aotaddon.util;

import com.aotaddon.access.DaotBridge;
import com.aotaddon.access.StaminaReflection;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Resilience Compound: +6000 max stamina, on top of whatever daot itself computes
 * as the baseline for that tick.
 *
 * daot's own tickShifterStamina() recomputes and overwrites playerMaxStamina every
 * tick for every current shifter, so this must run every tick too, AFTER daot's own
 * tick has already run - hence Post here, registered after daot's own listener in
 * mod-load order (NeoForge fires ServerTickEvent.Post listeners in registration
 * order within the same priority, so as long as daot registers its stamina tick
 * before this mod initializes, this runs after it - verify in-game; if daot's
 * stamina still looks unbuffed, daot's own tick may be running on a *different*
 * tick phase or later listener slot and this may need to move to a slightly
 * delayed check instead).
 *
 * Gated on: player has has_resilience tag AND is currently mounted on a
 * FemaleTitanEntity. Not gated at all when not shifted - daot's own baseline
 * calc already returns human-form numbers in that case, so there's nothing to
 * correct.
 */
public final class ResilienceStaminaHandler {

    private static final String UNLOCK_TAG = "has_resilience";
    private static final float STAMINA_BONUS = 6000f;

    private ResilienceStaminaHandler() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!player.getTags().contains(UNLOCK_TAG)) continue;
            if (!DaotBridge.isRidingFemaleTitan(player)) continue;

            StaminaReflection.applyMaxStaminaBonus(player.getUUID(), STAMINA_BONUS);
        }
    }
}
