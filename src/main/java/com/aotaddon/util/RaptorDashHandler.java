package com.aotaddon.util;

import com.aotaddon.AotAddon;
import com.aotaddon.access.DaotBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Raptor Compound - dash combo -> sound-barrier sprint burst.
 *
 * 5 successful dodges (validated by RaptorDashTrackingMixin, which only forwards
 * dodges that actually succeeded) within a 7-second rolling window triggers a
 * temporary speed burst on top of the nerfed 0.85 baseline. On expiry, reverts
 * specifically to 0.85 (the Raptor-nerfed baseline set by
 * RaptorCompoundTransformMixin) - NOT Female's original 1.4025 base - since 0.85
 * is what should remain in effect as the ongoing default while Raptor Compound
 * is active.
 */
public final class RaptorDashHandler {

    private static final int WINDOW_TICKS = 140; // 7 seconds
    private static final int REQUIRED_DASHES = 5;
    private static final int BURST_DURATION_TICKS = 60; // 3 seconds of burst speed, tune as needed
    private static final double BURST_ADD_VALUE = 1.15; // stacks on top of 0.85 base

    private static final ResourceLocation BURST_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "raptor_sound_barrier_burst");

    /** Per-player rolling window of dash tick-timestamps. */
    private static final Map<UUID, Deque<Long>> dashHistory = new HashMap<>();

    /** Per-player burst expiry tick, and the entity id of the titan it was applied to. */
    private static final Map<UUID, Long> burstExpiryTick = new HashMap<>();
    private static final Map<UUID, Integer> burstTitanEntityId = new HashMap<>();

    private RaptorDashHandler() {}

    public static void onSuccessfulDash(ServerPlayer player) {
        long now = player.level().getGameTime();
        UUID uuid = player.getUUID();

        Deque<Long> history = dashHistory.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        history.addLast(now);

        // Prune anything outside the rolling window
        while (!history.isEmpty() && now - history.peekFirst() > WINDOW_TICKS) {
            history.pollFirst();
        }

        if (history.size() >= REQUIRED_DASHES) {
            history.clear();
            triggerBurst(player, now);
        }
    }

    private static void triggerBurst(ServerPlayer player, long now) {
        Entity vehicle = DaotBridge.getFemaleTitanVehicle(player);
        if (!(vehicle instanceof LivingEntity female)) return;

        AttributeInstance speedAttribute = female.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        // Remove any existing burst modifier first (re-triggering refreshes duration
        // rather than stacking multiple bursts).
        speedAttribute.removeModifier(BURST_MODIFIER_ID);
        speedAttribute.addTransientModifier(new AttributeModifier(
                BURST_MODIFIER_ID, BURST_ADD_VALUE, AttributeModifier.Operation.ADD_VALUE
        ));

        UUID uuid = player.getUUID();
        burstExpiryTick.put(uuid, now + BURST_DURATION_TICKS);
        burstTitanEntityId.put(uuid, female.getId());

        AotAddon.LOGGER.info("[RaptorCompound] {} broke the sound barrier", player.getName().getString());
    }

    /** Registered as a server tick listener - expires bursts and reverts to the 0.85 baseline. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (burstExpiryTick.isEmpty()) return;

        long now = event.getServer().overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = burstExpiryTick.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now < entry.getValue()) continue;

            UUID uuid = entry.getKey();
            Integer entityId = burstTitanEntityId.get(uuid);
            it.remove();
            burstTitanEntityId.remove(uuid);

            if (entityId == null) continue;

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null) continue;

            Entity titan = player.level().getEntity(entityId);
            if (titan instanceof LivingEntity female) {
                AttributeInstance speedAttribute = female.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttribute != null) {
                    // Removing the transient burst modifier drops it back to
                    // whatever the base currently is - which RaptorCompoundTransformMixin
                    // already set to 0.85 at transform time, not Female's original 1.4025.
                    speedAttribute.removeModifier(BURST_MODIFIER_ID);
                }
            }
        }
    }

    /** Call when a player dismounts/reverts, to avoid stale tracking entries piling up. */
    public static void clear(UUID playerUUID) {
        dashHistory.remove(playerUUID);
        burstExpiryTick.remove(playerUUID);
        burstTitanEntityId.remove(playerUUID);
    }
}
