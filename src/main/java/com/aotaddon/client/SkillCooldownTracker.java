package com.aotaddon.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks active named cooldowns (Impulse, Dodge, future skills) for display
 * in SkillCooldownOverlay. Stores an absolute expiry timestamp per label
 * rather than a per-tick countdown, so remaining time is always computed
 * fresh from the wall clock when rendering - matches the expiry-timestamp
 * pattern already used by DodgeIFrameHandler/ImpulseWindowHandler.
 *
 * Call start(label, durationMs) right when a skill's cooldown begins.
 * The overlay purges expired entries itself each render call.
 */
public class SkillCooldownTracker {

    private static final Map<String, Long> expiryByLabel = new LinkedHashMap<>();

    public static void start(String label, long durationMs) {
        expiryByLabel.put(label, System.currentTimeMillis() + durationMs);
    }

    /**
     * @return active (label, remainingSeconds) pairs, longest remaining
     * first, with any expired entries removed from the tracker as a
     * side effect of calling this.
     */
    public static List<Map.Entry<String, Float>> getActiveSorted() {
        long now = System.currentTimeMillis();

        expiryByLabel.entrySet().removeIf(e -> e.getValue() <= now);

        List<Map.Entry<String, Float>> result = new ArrayList<>(expiryByLabel.size());
        for (Map.Entry<String, Long> e : expiryByLabel.entrySet()) {
            float remainingSeconds = (e.getValue() - now) / 1000f;
            result.add(Map.entry(e.getKey(), remainingSeconds));
        }

        result.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        return result;
    }
}