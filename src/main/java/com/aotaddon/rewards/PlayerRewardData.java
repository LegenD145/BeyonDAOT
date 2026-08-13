package com.aotaddon.rewards;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Persisted per-player reward state. Wire this up as a NeoForge AttachmentType
 * (Minecraft.getInstance / player.getData / player.setData depending on your
 * existing attachment registration pattern elsewhere in the addon) rather than
 * a bare capability, matching how other persistent player data is stored.
 *
 * honorPoints is a float/double specifically because per-kill rates like
 * 0.35 and 0.25 are common — never store this as an int.
 */
public class PlayerRewardData {

    private double honorPoints = 0.0;
    private final Set<ResourceLocation> firstKillGranted = new HashSet<>();

    public double getHonorPoints() {
        return honorPoints;
    }

    public void addHonor(double amount) {
        this.honorPoints += amount;
    }

    /**
     * Returns true if this is the FIRST time this entity type's bonus is being claimed
     * (and marks it granted, persistently, so it never re-triggers even across relogs).
     * Call this once per applicable kill; false means the reduced repeat rate applies.
     */
    public boolean claimFirstKillBonus(ResourceLocation entityTypeId) {
        return firstKillGranted.add(entityTypeId);
    }

    public boolean hasClaimedFirstKillBonus(ResourceLocation entityTypeId) {
        return firstKillGranted.contains(entityTypeId);
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putDouble("HonorPoints", honorPoints);
        ListTag list = new ListTag();
        for (ResourceLocation id : firstKillGranted) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("FirstKillGranted", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.honorPoints = tag.getDouble("HonorPoints");
        this.firstKillGranted.clear();
        ListTag list = tag.getList("FirstKillGranted", StringTag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            this.firstKillGranted.add(ResourceLocation.parse(list.getString(i)));
        }
    }
}
