package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.world.entity.Entity;

/**
 * Shared reflection check for daot's ShifterTitan marker interface - used both
 * server-side (ShiftlockStateHandler, BastionStateHandler) and client-side
 * (ShiftlockClientTick) so there's one place to fix if daot ever renames this.
 */
public final class ShifterTitanUtil {

    private ShifterTitanUtil() {}

    public static boolean isShifterTitan(Entity e) {
        if (e == null) return false;
        try {
            return Class.forName("daot.ShifterTitan").isInstance(e);
        } catch (ClassNotFoundException ex) {
            AotAddon.LOGGER.warn("[ShifterTitanUtil] daot.ShifterTitan class not found via reflection!", ex);
            return false;
        }
    }
}