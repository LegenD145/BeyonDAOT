package com.aotaddon.access;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Reflection bridge into daot.network.ModNetworking's private static stamina maps.
 *
 * playerMaxStamina is declared `private static final Map<UUID, Float>` - since it's
 * final, the Map *reference* never changes, only its contents. So we only need to
 * resolve the Field once (cached, same pattern as DaotGasReflection) and can then
 * hold onto the live Map instance directly and mutate it like a normal map every
 * tick without repeating the reflection lookup.
 *
 * IMPORTANT: DAOT's own tickShifterStamina() overwrites playerMaxStamina.put(uuid, ...)
 * every single server tick for every current shifter (confirmed in ModNetworking
 * around the passive stamina drain/regen loop). Any correction made here must run
 * from a tick handler registered to fire AFTER daot's own tick, and must run every
 * tick - a one-time write will get stomped within a tick or two.
 */
public final class StaminaReflection {

    private static final String MOD_NETWORKING_CLASS = "daot.network.ModNetworking";

    private static Map<UUID, Float> playerMaxStaminaMap;
    private static Method getMaxStaminaForPlayerMethod;
    private static Class<?> serverPlayerDaotClass;
    private static boolean initFailed = false;

    private StaminaReflection() {}

    @SuppressWarnings("unchecked")
    private static void init() {
        if (playerMaxStaminaMap != null || initFailed) {
            return;
        }
        try {
            Class<?> modNetworkingClass = Class.forName(MOD_NETWORKING_CLASS);

            Field maxStaminaField = modNetworkingClass.getDeclaredField("playerMaxStamina");
            maxStaminaField.setAccessible(true);
            playerMaxStaminaMap = (Map<UUID, Float>) maxStaminaField.get(null);

            getMaxStaminaForPlayerMethod = modNetworkingClass.getMethod(
                    "getMaxStaminaForUUID", UUID.class);
        } catch (Exception e) {
            initFailed = true;
            System.err.println("[aotaddon] Failed to bind to daot stamina fields via reflection: " + e);
        }
    }

    /**
     * daot's own baseline max stamina for this player right now (before our bonus),
     * as recomputed by daot's own getMaxStaminaForUUID. Returns -1 if unavailable.
     */
    public static float getBaselineMax(UUID playerUUID) {
        init();
        if (initFailed) return -1f;
        try {
            return (float) getMaxStaminaForPlayerMethod.invoke(null, playerUUID);
        } catch (Exception e) {
            return -1f;
        }
    }

    /**
     * Adds `bonus` on top of whatever value is currently stored in daot's own
     * playerMaxStamina map for this player. Call every tick, after daot's own
     * stamina tick has run, for as long as the bonus should apply.
     */
    public static void applyMaxStaminaBonus(UUID playerUUID, float bonus) {
        init();
        if (initFailed || playerMaxStaminaMap == null) return;
        Float current = playerMaxStaminaMap.get(playerUUID);
        if (current == null) return;
        playerMaxStaminaMap.put(playerUUID, current + bonus);
    }
}
