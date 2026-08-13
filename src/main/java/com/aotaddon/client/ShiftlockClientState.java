package com.aotaddon.client;

/**
 * Client-side mirror of whether shiftlock is currently active for the local player.
 * Set from the server's confirmation (ShiftlockStateSyncPayload) rather than optimistically
 * on keypress, so a rejected toggle (e.g. "not in titan form") can't desync client vs server.
 */
public final class ShiftlockClientState {

    private static volatile boolean active = false;

    private ShiftlockClientState() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}