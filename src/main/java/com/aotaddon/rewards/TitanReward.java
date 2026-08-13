package com.aotaddon.rewards;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Standard per-kill reward for a titan type.
 * honor/combatPoints are floats since Honor Points in particular use sub-1 values (0.35, 0.25, etc).
 */
public record TitanReward(float honor, float combatPoints, int medals, int paradisRep, int marleyRep) {

    /** Entities with their own one-time "first kill" Honor bonus + reduced repeat rate,
     *  handled separately in {@link TitanKillRewardHandler} rather than in this table. */
    public static final ResourceLocation OGRE = ResourceLocation.parse("dannys-aot:ogre_titan");
    public static final ResourceLocation ABNORMAL_TITAN = ResourceLocation.parse("dannys-aot:abnormal_titan");
    public static final ResourceLocation CRAWLING_ABNORMAL_TITAN = ResourceLocation.parse("dannys-aot:crawling_abnormal_titan");

    private static ResourceLocation id(String path) {
        return ResourceLocation.parse("dannys-aot:" + path);
    }

    public static final Map<ResourceLocation, TitanReward> TABLE = Map.ofEntries(
            Map.entry(id("titan"),                   new TitanReward(0.35f, 0.50f, 10, 3, 0)),
            Map.entry(id("small_titan"),              new TitanReward(0.35f, 0.50f, 10, 3, 0)),
            Map.entry(id("small_titan_2"),            new TitanReward(0.35f, 0.50f, 10, 3, 0)),
            Map.entry(id("sad_titan"),                new TitanReward(0.35f, 0.50f, 10, 3, 0)),
            Map.entry(id("titan_beard"),              new TitanReward(0.35f, 0.50f, 10, 3, -4)),
            Map.entry(id("titan_tropical"),           new TitanReward(0.35f, 0.50f, 10, 3, 0)),
            Map.entry(id("yellow_titan"),             new TitanReward(0.35f, 0.50f, 10, 3, 0)),
            Map.entry(id("fritz_titan"),              new TitanReward(1.00f, 3.00f, 40, 15, -10)),
            Map.entry(id("connie_father"),            new TitanReward(0.35f, 0.50f, 10, 3, -3)),
            Map.entry(id("abnormal_titan"),           new TitanReward(0.50f, 1.25f, 20, 7, 0)),
            Map.entry(id("crawler_titan"),            new TitanReward(0.50f, 1.25f, 20, 7, 0)),
            Map.entry(id("crawling_abnormal_titan"),  new TitanReward(0.50f, 1.25f, 20, 7, 0))
            // TODO: add Ogre's standard row here once confirmed whether it has one
            // beyond the first-kill bonus handled in TitanKillRewardHandler.
    );

    public static TitanReward forEntity(ResourceLocation entityTypeId) {
        return TABLE.get(entityTypeId);
    }
}