package com.aotaddon.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

public final class DaotBridge {

    private DaotBridge() {
    }

    public static boolean isCuffed(Entity entity) {
        try {
            Class<?> trackerClass = Class.forName("daot.HandcuffsTracker");
            Method isCuffedMethod = trackerClass.getMethod("isCuffed", Entity.class);
            Object result = isCuffedMethod.invoke(null, entity);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * True if the player is currently mounted on (i.e. shifted into) a
     * daot.FemaleTitanEntity. Fresh reflection every call, no caching - matches
     * the established convention for entity-instance checks (as opposed to the
     * cached Class/Method lookups used for hot-path static-map access).
     */
    public static boolean isRidingFemaleTitan(ServerPlayer player) {
        try {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) return false;
            Class<?> femaleTitanClass = Class.forName("daot.FemaleTitanEntity");
            return femaleTitanClass.isInstance(vehicle);
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns the entity the player is riding if it's a daot.FemaleTitanEntity, else null. */
    public static Entity getFemaleTitanVehicle(ServerPlayer player) {
        try {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) return null;
            Class<?> femaleTitanClass = Class.forName("daot.FemaleTitanEntity");
            return femaleTitanClass.isInstance(vehicle) ? vehicle : null;
        } catch (Exception e) {
            return null;
        }
    }
}