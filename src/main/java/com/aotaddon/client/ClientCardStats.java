package com.aotaddon.client;

/**
 * Client copy of family, bloodline, and combat XP for the book Stats page.
 * Filled by PlayerCardSyncPayload — do not read persistentData on the client.
 */
public final class ClientCardStats {

    private static String family = "";
    private static String bloodline = "";
    private static double combatXp = 0.0;

    private ClientCardStats() {}

    public static String getFamily() {
        return family;
    }

    public static String getBloodline() {
        return bloodline;
    }

    public static double getCombatXp() {
        return combatXp;
    }

    public static void set(String newFamily, String newBloodline, double newCombatXp) {
        family = newFamily == null ? "" : newFamily;
        bloodline = newBloodline == null ? "" : newBloodline;
        combatXp = newCombatXp;
    }
}
