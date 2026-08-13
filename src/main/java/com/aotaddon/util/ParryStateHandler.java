package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Detects the exact moment a player starts blocking with ODM blades (daot's
 * BladeBlockTracker.isBlocking rising edge: false -> true) and opens a short
 * parry window from that instant. Polls daot's own blocking state every tick
 * rather than hooking into whatever triggers it - daot already handles the
 * block input/cooldown/slowdown entirely; we just piggyback on the result.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class ParryStateHandler {

    /** 0.4 seconds at 20 TPS */
    private static final int PARRY_WINDOW_TICKS = 8;

    private static final Map<UUID, Boolean> wasBlockingLastTick = new HashMap<>();
    private static final Map<UUID, Long> parryWindowEndTick = new HashMap<>();

    private static Method isBlockingMethod;
    private static boolean reflectionFailed = false;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** True if the player is currently within their 0.4s post-block-start parry window. */
    public static boolean isInParryWindow(UUID uuid, long currentTick) {
        Long endTick = parryWindowEndTick.get(uuid);
        return endTick != null && currentTick <= endTick;
    }

    public static void onPlayerDisconnect(UUID uuid) {
        wasBlockingLastTick.remove(uuid);
        parryWindowEndTick.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Server tick — edge detection
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            boolean isBlockingNow = isBlocking(player);
            boolean wasBlocking = wasBlockingLastTick.getOrDefault(uuid, false);

            if (isBlockingNow && !wasBlocking) {
                // Rising edge: block just started - open the parry window.
                parryWindowEndTick.put(uuid, currentTick + PARRY_WINDOW_TICKS);
                AotAddon.LOGGER.info("[Parry] {} opened parry window (tick {}-{})",
                        player.getName().getString(), currentTick, currentTick + PARRY_WINDOW_TICKS);
            }

            wasBlockingLastTick.put(uuid, isBlockingNow);
        }
    }

    // -------------------------------------------------------------------------
    // Reflection into daot.BladeBlockTracker
    // -------------------------------------------------------------------------

    private static boolean isBlocking(ServerPlayer player) {
        try {
            if (isBlockingMethod == null && !reflectionFailed) {
                Class<?> trackerClass = Class.forName("daot.BladeBlockTracker");
                isBlockingMethod = trackerClass.getMethod("isBlocking", ServerPlayer.class);
            }
            if (reflectionFailed) return false;
            return (boolean) isBlockingMethod.invoke(null, player);
        } catch (Exception e) {
            if (!reflectionFailed) {
                AotAddon.LOGGER.warn("[Parry] Failed to bind to daot.BladeBlockTracker.isBlocking", e);
                reflectionFailed = true;
            }
            return false;
        }
    }
}
