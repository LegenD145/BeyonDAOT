package com.aotaddon.client;

/**
 * Client copy of family, bloodline, combat XP, helos kills, skill tree levels,
 * and reputation. Filled by PlayerCardSyncPayload.
 */
public final class ClientCardStats {

    private static String family = "";
    private static String bloodline = "";
    private static double combatXp = 0.0;
    private static int helosKills = 0;
    private static int repParadis = 0;
    private static int repMarley = 0;
    private static boolean grabMode = false;
    private static boolean consentOpen = false;

    private ClientCardStats() {}

    public static String getFamily() { return family; }
    public static String getBloodline() { return bloodline; }
    public static double getCombatXp() { return combatXp; }
    public static int getHelosKills() { return helosKills; }
    public static int getRepParadis() { return repParadis; }
    public static int getRepMarley() { return repMarley; }
    public static boolean isGrabMode() { return grabMode; }
    public static boolean isConsentOpen() { return consentOpen; }

    public static void set(String newFamily, String newBloodline, double newCombatXp, int newHelosKills,
                           int newRepParadis, int newRepMarley,
                           boolean newGrabMode, boolean newConsentOpen) {
        family = newFamily == null ? "" : newFamily;
        bloodline = newBloodline == null ? "" : newBloodline;
        combatXp = newCombatXp;
        helosKills = newHelosKills;
        repParadis = newRepParadis;
        repMarley = newRepMarley;
        grabMode = newGrabMode;
        consentOpen = newConsentOpen;
    }
}
