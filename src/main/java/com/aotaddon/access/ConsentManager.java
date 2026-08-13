package com.aotaddon.access;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConsentManager extends SavedData {

    private static final String DATA_NAME = "titanrequiem_consent";

    private final Map<UUID, Boolean> consentOpen = new HashMap<>();

    public static final SavedData.Factory<ConsentManager> FACTORY =
            new SavedData.Factory<>(ConsentManager::new, ConsentManager::load, null);

    public ConsentManager() {
    }

    public static ConsentManager get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean isOpen(Player player) {
        return isOpen(player.getUUID());
    }

    public boolean isOpen(UUID uuid) {
        return consentOpen.getOrDefault(uuid, false);
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUUID();
        boolean newState = !isOpen(uuid);
        consentOpen.put(uuid, newState);
        setDirty();
        return newState;
    }

    public void set(UUID uuid, boolean state) {
        consentOpen.put(uuid, state);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : consentOpen.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("uuid", entry.getKey());
            list.add(entryTag);
        }
        tag.put("consentOpen", list);
        return tag;
    }

    private static ConsentManager load(CompoundTag tag, HolderLookup.Provider provider) {
        ConsentManager manager = new ConsentManager();
        ListTag list = tag.getList("consentOpen", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            UUID uuid = entryTag.getUUID("uuid");
            manager.consentOpen.put(uuid, true);
        }
        return manager;
    }
}