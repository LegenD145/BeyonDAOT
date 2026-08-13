package com.aotaddon.currency;

import com.aotaddon.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-wide medal stock — persists with the world via NeoForge SavedData.
 * Updated whenever a player deposits medals.
 */
public class SystemStockData extends SavedData {

    private static final String SAVED_DATA_KEY = "aotaddon_medal_stock";

    private static final String KEY_ROYAL  = "stock_royal";
    private static final String KEY_GOLD   = "stock_gold";
    private static final String KEY_SILVER = "stock_silver";
    private static final String KEY_BRONZE = "stock_bronze";

    private int royalCount;
    private int goldCount;
    private int silverCount;
    private int bronzeCount;

    public SystemStockData() {}

    // =========================================================================
    // SAVE / LOAD
    // =========================================================================

    public static SystemStockData load(CompoundTag tag, HolderLookup.Provider provider) {
        SystemStockData data = new SystemStockData();
        data.royalCount  = tag.getInt(KEY_ROYAL);
        data.goldCount   = tag.getInt(KEY_GOLD);
        data.silverCount = tag.getInt(KEY_SILVER);
        data.bronzeCount = tag.getInt(KEY_BRONZE);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(KEY_ROYAL,  royalCount);
        tag.putInt(KEY_GOLD,   goldCount);
        tag.putInt(KEY_SILVER, silverCount);
        tag.putInt(KEY_BRONZE, bronzeCount);
        return tag;
    }

    // =========================================================================
    // ACCESS
    // =========================================================================

    public static SystemStockData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        SystemStockData::new,
                        SystemStockData::load
                ),
                SAVED_DATA_KEY
        );
    }

    public void addMedals(Map<Item, Integer> medals) {
        for (Map.Entry<Item, Integer> entry : medals.entrySet()) {
            Item item  = entry.getKey();
            int  count = entry.getValue();
            if      (item == ModItems.ROYAL_MEDAL.get())  royalCount  += count;
            else if (item == ModItems.GOLD_MEDAL.get())   goldCount   += count;
            else if (item == ModItems.SILVER_MEDAL.get()) silverCount += count;
            else if (item == ModItems.BRONZE_MEDAL.get()) bronzeCount += count;
        }
        setDirty();
    }

    public Map<Item, Integer> getStockMap() {
        Map<Item, Integer> map = new LinkedHashMap<>();
        map.put(ModItems.ROYAL_MEDAL.get(),  royalCount);
        map.put(ModItems.GOLD_MEDAL.get(),   goldCount);
        map.put(ModItems.SILVER_MEDAL.get(), silverCount);
        map.put(ModItems.BRONZE_MEDAL.get(), bronzeCount);
        return map;
    }

    public int getTotalValue() {
        return (royalCount * 100) + (goldCount * 25) + (silverCount * 10) + bronzeCount;
    }
}