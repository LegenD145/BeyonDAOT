package com.aotaddon.pd;

/**
 * Client-side cached copy of the local player's current life count (0-3).
 * Placeholder — nothing writes to this yet. Once LifeData exists server-side,
 * sync it here on change + on login, same pattern as ClientHonorData.
 * Defaults to 3 so the heart renderer shows red (normal) until real data arrives.
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