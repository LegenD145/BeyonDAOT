package com.aotaddon.util;

import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks a short server-side window after a player's Impulse (double-tap W)
 * dash fires. ODMSkillHandler that actually detects and applies the dash is
 * @OnlyIn(Dist.CLIENT) - the only signal the server gets is the existing
 * SkillEffectPayload(skillId=0) sent right after Impulse fires client-side.
 * This class turns that one-shot payload into a short-lived server flag,
 * the same way DodgeStartPayload -> DodgeIFrameHandler already does for
 * Dodge i-frames.
 *
 * Used to gate the Female Titan decapitation check: a qualifying eye hit
 * only counts as a decapitation if it lands within this window of the
 * player's last Impulse.
 */
public class ImpulseWindowHandler {

    /** UUID -> expiry timestamp (System.currentTimeMillis()) */
    private static final Map<UUID, Long> impulseExpiry = new HashMap<>();

    /** Default window if config isn't loaded yet */
    private static final long DEFAULT_WINDOW_MS = 600;

    public static void markImpulseFired(UUID uuid) {
        long durationMs;
        try {
            durationMs = AddonConfig.IMPULSE_DECAP_WINDOW_MS.get();
        } catch (IllegalStateException e) {
            durationMs = DEFAULT_WINDOW_MS;
        }

        long expiry = System.currentTimeMillis() + durationMs;
        impulseExpiry.put(uuid, expiry);

        AotAddon.LOGGER.debug("[ImpulseWindow] {} has a {}ms decap window", uuid, durationMs);
    }

    public static boolean isWithinImpulseWindow(UUID uuid) {
        Long expiry = impulseExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            impulseExpiry.remove(uuid);
            return false;
        }
        return true;
    }
}