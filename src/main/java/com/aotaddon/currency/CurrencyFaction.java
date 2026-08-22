package com.aotaddon.currency;

import com.aotaddon.AotAddon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Wallet nation: medals (Eldia) vs banknotes (Marley).
 * Residence only — bloodline does not affect currency.
 */
public final class CurrencyFaction {

    public enum Faction {
        ELDIAN, MARLEY, NONE
    }

    private static Method getDataMethod;
    private static Method getBloodlineMethod;
    private static boolean bound;
    private static boolean bindFailed;

    private CurrencyFaction() {}

    public static Faction get(Player player) {
        return switch (ResidenceData.get(player)) {
            case "eldia" -> Faction.ELDIAN;
            case "marley" -> Faction.MARLEY;
            default -> Faction.NONE;
        };
    }

    /** DAOT enum name, or empty if unset / lookup failed. */
    public static String readName(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return "";
        }
        bind();
        if (!bound) {
            return "";
        }
        try {
            Object bloodlineData = getDataMethod.invoke(null, serverPlayer.getServer());
            if (bloodlineData == null) {
                return "";
            }
            Object bloodlineType = getBloodlineMethod.invoke(bloodlineData, serverPlayer.getUUID());
            if (!(bloodlineType instanceof Enum<?> type)) {
                return "";
            }
            return type.name().toUpperCase();
        } catch (Exception e) {
            AotAddon.LOGGER.debug("[AotAddon] DAOT bloodline lookup failed: {}", e.getMessage());
            return "";
        }
    }

    private static void bind() {
        if (bound || bindFailed) {
            return;
        }
        try {
            Class<?> bloodlineDataClass = Class.forName("daot.BloodlineData");
            getDataMethod = bloodlineDataClass.getMethod("get", MinecraftServer.class);
            getBloodlineMethod = bloodlineDataClass.getMethod("getBloodline", UUID.class);
            bound = true;
        } catch (Exception e) {
            bindFailed = true;
            AotAddon.LOGGER.error("[AotAddon] Failed to bind daot.BloodlineData: {}", e.getMessage());
        }
    }
}
