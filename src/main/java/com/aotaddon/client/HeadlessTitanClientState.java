package com.aotaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side render state for titan bodies that have already had their
 * detached head spawned. This is intentionally visual-only; gameplay still
 * lives on the server.
 */
public final class HeadlessTitanClientState {

    private static final long STATE_TTL_TICKS = 20L * 30L;
    private static final Map<Integer, Long> HEADLESS_UNTIL = new HashMap<>();

    private HeadlessTitanClientState() {
    }

    public static void markHeadless(int entityId) {
        long now = currentGameTime();
        HEADLESS_UNTIL.put(entityId, now + STATE_TTL_TICKS);
        pruneExpired(now);
    }

    public static boolean isHeadlessFemaleTitan(Entity entity) {
        if (entity == null || !entity.getClass().getName().equals("daot.FemaleTitanEntity")) {
            return false;
        }

        long now = currentGameTime();
        Long until = HEADLESS_UNTIL.get(entity.getId());
        if (until == null) {
            return false;
        }

        if (until < now) {
            HEADLESS_UNTIL.remove(entity.getId());
            return false;
        }

        return true;
    }

    private static long currentGameTime() {
        if (Minecraft.getInstance().level == null) {
            return 0L;
        }
        return Minecraft.getInstance().level.getGameTime();
    }

    private static void pruneExpired(long now) {
        Iterator<Map.Entry<Integer, Long>> iterator = HEADLESS_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now) {
                iterator.remove();
            }
        }
    }
}
