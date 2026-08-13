package com.aotaddon.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side only. RewardPopupManager.push(List<String>) is called from
 * RewardPopupPayload.handle() when a reward packet arrives from the server.
 */
public final class RewardPopupManager {

    private static final long LIFETIME_MS = 1800L;
    private static final long FADE_START_MS = 1200L;

    private record ActiveGroup(List<String> lines, long spawnedAt) {}

    private static final List<ActiveGroup> ACTIVE = new ArrayList<>();

    private RewardPopupManager() {}

    public static void push(List<String> lines) {
        if (lines == null || lines.isEmpty()) return;
        ACTIVE.add(new ActiveGroup(new ArrayList<>(lines), System.currentTimeMillis()));
    }

    /** Call once per frame before rendering; drops expired groups. */
    public static List<ActiveGroupView> tickAndGetVisible() {
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(g -> now - g.spawnedAt() > LIFETIME_MS);

        List<ActiveGroupView> visible = new ArrayList<>();
        for (ActiveGroup g : ACTIVE) {
            long age = now - g.spawnedAt();
            float alpha = age <= FADE_START_MS
                    ? 1.0f
                    : 1.0f - ((float) (age - FADE_START_MS) / (LIFETIME_MS - FADE_START_MS));
            visible.add(new ActiveGroupView(g.lines(), Math.max(0f, Math.min(1f, alpha))));
        }
        return visible;
    }

    public record ActiveGroupView(List<String> lines, float alpha) {}
}