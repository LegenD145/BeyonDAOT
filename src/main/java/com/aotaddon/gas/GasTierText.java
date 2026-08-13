package com.aotaddon.gas;

/**
 * Resolves a gas amount into one of the four flavor-text tiers.
 * Breakpoints are expressed as ratios of max gas rather than hardcoded to 500,
 * so this still behaves sensibly for APG gear (max 1000) without any % ever being shown.
 */
public final class GasTierText {

    private GasTierText() {}

    public static String resolve(int gas, int maxGas) {
        if (maxGas <= 0) {
            return null;
        }
        float ratio = (float) gas / (float) maxGas;

        if (ratio >= 0.70f) {
            // 500-350 out of 500
            return "My gear is hard to shake, I seem to have more than enough.";
        } else if (ratio >= 0.50f) {
            // 349-250 out of 500
            return "I can slowly feel my gear being loose. Maybe I need to refill now.";
        } else if (ratio >= 0.20f) {
            // 249-100 out of 500
            return "I can't seem to feel my gear at all, I need to find a refilling station.";
        } else {
            // 99-0 out of 500
            return "Shit, I think I might be out of gas.";
        }
    }
}