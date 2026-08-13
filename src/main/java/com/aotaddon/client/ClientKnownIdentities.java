package com.aotaddon.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side cached mirror of the local player's known-identities set, kept
 * in sync via RevealIdentityPayload (incremental) and IdentityFullSyncPayload
 * (on login). Read by IdentityNametagHandler to decide whether a given
 * player's nametag is allowed to render.
 *
 * Never trust this for anything security-sensitive — it's purely a display
 * gate. The actual source of truth is server-side persistentData
 * (IdentityRevealData); a modified client could always ignore this cache.
 */
public final class ClientKnownIdentities {

    private static final Set<UUID> known = Collections.synchronizedSet(new HashSet<>());

    private ClientKnownIdentities() {}

    public static boolean knows(UUID uuid) {
        return known.contains(uuid);
    }

    public static void reveal(UUID uuid) {
        known.add(uuid);
    }

    /** Replaces the entire cache — used for the login full-sync. */
    public static void setAll(Collection<UUID> uuids) {
        known.clear();
        known.addAll(uuids);
    }

    /** Called on disconnect so a relog to a different server/world starts clean. */
    public static void clear() {
        known.clear();
    }
}
