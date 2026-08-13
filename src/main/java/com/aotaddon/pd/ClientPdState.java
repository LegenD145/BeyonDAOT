package com.aotaddon.pd;

/**
 * Client-side cached copy of the LOCAL player's currently resolved PD state.
 * Placeholder cache only — nothing writes to this yet. Once the server-side
 * PdState/command system exists, sync it here the same way HonorSyncPayload
 * feeds ClientHonorData (sync on change + sync on login).
 */
public final class ClientPdState {

    private static PdType current = PdType.NONE;

    private ClientPdState() {}

    public static PdType get() {
        return current;
    }

    public static void set(PdType type) {
        current = type;
    }
}