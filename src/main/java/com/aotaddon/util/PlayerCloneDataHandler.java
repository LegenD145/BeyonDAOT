package com.aotaddon.util;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Copies persistent player data from the dead player entity to the respawned clone.
 * This keeps addon systems backed by persistentData alive across normal death respawns.
 */
public final class PlayerCloneDataHandler {

    private PlayerCloneDataHandler() {}

    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        CompoundTag oldData = event.getOriginal().getPersistentData().copy();
        event.getEntity().getPersistentData().merge(oldData);
    }
}
