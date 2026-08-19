package com.aotaddon.pd;

/**
 * Client-side cached copy of the local player's current life count (0-3).
 * Updated by PdLifeSyncPayload. Defaults to 3 until first sync arrives.
 */
public final class ClientLifeState {

    private static int lives = 3;

    private ClientLifeState() {}

    public static int getLives() {
        return lives;
    }

    public static void setLives(int newLives) {
        lives = newLives;
    }
}