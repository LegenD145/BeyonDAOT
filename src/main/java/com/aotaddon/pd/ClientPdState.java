package com.aotaddon.pd;

/**
 * Client-side cached copy of the LOCAL player's currently resolved PD state.
 * Updated by PdLifeSyncPayload on login and periodic sync.
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