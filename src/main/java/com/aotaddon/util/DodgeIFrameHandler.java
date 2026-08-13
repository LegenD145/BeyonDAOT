package com.aotaddon.util;

import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which players currently have Dodge i-frames active and cancels
 * incoming damage for them during that window.
 *
 * Purely event-driven — no server tick loop. Expiry is checked lazily
 * whenever a damage event actually fires for that player, and stale
 * entries are cleaned up opportunistically.
 *
 * NOT annotated with @SubscribeEvent — onIncomingDamage is registered
 * manually via NeoForge.EVENT_BUS.addListener() in AotAddon, so
 * LivingIncomingDamageEvent (which references LivingEntity) is never
 * loaded at class-load time. Same pattern as FamilyEventHandler.
 *
 * Does NOT interfere with Danny's grab/mount logic — grabs are not damage
 * events, they're entity mounting, so an already-grabbed player dodging
 * will not break free. This is intentional per design.
 */
public class DodgeIFrameHandler {

    /** UUID -> expiry timestamp (System.currentTimeMillis()) */
    private static final Map<UUID, Long> iframeExpiry = new HashMap<>();

    /** Default i-frame duration if config isn't loaded yet (matches Freedom War's 0.8s) */
    private static final long DEFAULT_IFRAME_MS = 800;

    public static void grantIFrames(ServerPlayer player) {
        long durationMs;
        try {
            durationMs = AddonConfig.ODM_DODGE_IFRAME_MS.get();
        } catch (IllegalStateException e) {
            durationMs = DEFAULT_IFRAME_MS;
        }

        long expiry = System.currentTimeMillis() + durationMs;
        iframeExpiry.put(player.getUUID(), expiry);

        AotAddon.LOGGER.debug("[DodgeIFrame] {} granted {}ms i-frames",
                player.getName().getString(), durationMs);
    }

    public static boolean hasIFrames(UUID uuid) {
        Long expiry = iframeExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            iframeExpiry.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Called via NeoForge.EVENT_BUS.addListener(DodgeIFrameHandler::onIncomingDamage)
     * in AotAddon — not auto-subscribed, to avoid early-loading LivingEntity.
     */
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (hasIFrames(player.getUUID())) {
            event.setCanceled(true);
        }
    }
}
