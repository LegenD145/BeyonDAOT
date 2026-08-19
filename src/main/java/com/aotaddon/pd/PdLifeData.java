package com.aotaddon.pd;

import net.minecraft.world.entity.player.Player;

/**
 * Server-side reader for PD/lives state from persistent data.
 * Designed to tolerate multiple legacy key names until the source system is unified.
 */
public final class PdLifeData {
    private static final String[] PD_KEYS = {"pdType", "pd_type", "pd_state", "pd"};
    private static final String[] LIVES_KEYS = {"lives", "extraLives", "lifeCount"};

    private PdLifeData() {}

    public static PdType getPdType(Player player) {
        for (String key : PD_KEYS) {
            String raw = player.getPersistentData().getString(key);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            return parsePdType(raw);
        }
        return PdType.NONE;
    }

    public static int getLives(Player player) {
        for (String key : LIVES_KEYS) {
            if (player.getPersistentData().contains(key)) {
                return clampLives(player.getPersistentData().getInt(key));
            }
        }
        return 3;
    }

    private static PdType parsePdType(String raw) {
        String value = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (value) {
            case "PD" -> PdType.PD;
            case "SOFT_PD", "SOFTPD" -> PdType.SOFT_PD;
            case "EXTINCTION", "EXTINCT" -> PdType.EXTINCTION;
            default -> PdType.NONE;
        };
    }

    private static int clampLives(int value) {
        return Math.max(0, Math.min(3, value));
    }
}
