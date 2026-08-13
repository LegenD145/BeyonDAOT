package com.aotaddon.identity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player "who have I learned the identity of" set — anti-metagaming
 * system. Stored in the OBSERVER's persistentData (same convention as
 * FamilyData), keyed by the SPEAKER's UUID they revealed themselves to.
 *
 * This is deliberately asymmetric: revealing your name to player A does not
 * reveal it to player B. Each observer's set is independent and only grows
 * when they are within IdentityRevealHandler.REVEAL_RADIUS of a trigger
 * phrase at the moment it's said.
 */
public class IdentityRevealData {

    private static final String KEY_KNOWN_IDENTITIES = "aotaddon_known_identities";

    /** Returns the set of speaker UUIDs this observer has learned the identity of. */
    public static Set<UUID> getKnownIdentities(Player observer) {
        Set<UUID> known = new HashSet<>();
        ListTag list = observer.getPersistentData().getList(KEY_KNOWN_IDENTITIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                known.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Corrupt/legacy entry — skip rather than crash the whole set.
            }
        }
        return known;
    }

    public static boolean knows(Player observer, UUID speakerUuid) {
        ListTag list = observer.getPersistentData().getList(KEY_KNOWN_IDENTITIES, Tag.TAG_STRING);
        String target = speakerUuid.toString();
        for (int i = 0; i < list.size(); i++) {
            if (target.equals(list.getString(i))) return true;
        }
        return false;
    }

    /**
     * Marks speakerUuid as known to observer. Returns true if this was a NEW
     * reveal (so the caller knows whether a sync packet is actually needed),
     * false if the observer already knew them.
     */
    public static boolean reveal(Player observer, UUID speakerUuid) {
        if (knows(observer, speakerUuid)) return false;

        CompoundTag data = observer.getPersistentData();
        ListTag list = data.getList(KEY_KNOWN_IDENTITIES, Tag.TAG_STRING);
        list.add(StringTag.valueOf(speakerUuid.toString()));
        data.put(KEY_KNOWN_IDENTITIES, list);
        return true;
    }
}
