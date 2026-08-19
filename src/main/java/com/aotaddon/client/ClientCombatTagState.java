package com.aotaddon.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientCombatTagState {

    private static int secondsLeft = 0;

    public static void setSecondsLeft(int seconds) {
        secondsLeft = seconds;
    }

    public static int getSecondsLeft() {
        return secondsLeft;
    }

    public static boolean isInCombat() {
        return secondsLeft > 0;
    }
}
