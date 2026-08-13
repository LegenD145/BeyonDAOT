package com.aotaddon.currency;

import net.minecraft.world.entity.player.Player;

/**
 * Reads the same persistentData "bloodline" key your reputation.js script
 * writes (via /setbloodline) - keeps Java and KubeJS reading the exact same
 * source of truth rather than duplicating state.
 */
public final class CurrencyFaction {

    private CurrencyFaction() {
    }

    public enum Faction {
        ELDIAN, MARLEY, NONE
    }

    public static Faction get(Player player) {
        String bloodline = player.getPersistentData().getString("bloodline");
        if ("eldian".equals(bloodline)) {
            return Faction.ELDIAN;
        }
        if ("marley".equals(bloodline)) {
            return Faction.MARLEY;
        }
        // neutral / helos / ackerman / yeager / unset all fall here - blocked
        // from both currencies per design.
        return Faction.NONE;
    }
}