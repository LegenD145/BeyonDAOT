package com.aotaddon.currency;

import com.aotaddon.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared medal logic — value mapping, greedy algorithm, inventory scanning.
 * Denominations in descending order: Royal=100, Gold=25, Silver=10, Bronze=1
 */
public class MedalHelper {

    // Ordered highest → lowest for greedy algorithm
    public static final LinkedHashMap<Item, Integer> MEDAL_VALUES = new LinkedHashMap<>();

    static {
        MEDAL_VALUES.put(ModItems.ROYAL_MEDAL.get(),  100);
        MEDAL_VALUES.put(ModItems.GOLD_MEDAL.get(),    25);
        MEDAL_VALUES.put(ModItems.SILVER_MEDAL.get(),  10);
        MEDAL_VALUES.put(ModItems.BRONZE_MEDAL.get(),   1);
    }

    /**
     * Counts total medal value in a player's inventory.
     */
    public static int countInventoryValue(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            Integer val = MEDAL_VALUES.get(stack.getItem());
            if (val != null) total += val * stack.getCount();
        }
        return total;
    }

    /**
     * Counts how many of each medal type the player has in inventory.
     * Returns map of Item → count.
     */
    public static Map<Item, Integer> countInventoryMedals(Player player) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Item medal : MEDAL_VALUES.keySet()) counts.put(medal, 0);
        for (ItemStack stack : player.getInventory().items) {
            Integer val = MEDAL_VALUES.get(stack.getItem());
            if (val != null) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    /**
     * Checks if the player's inventory contains medals that add up to EXACTLY the requested amount.
     * Returns the combination to take, or null if no exact match exists.
     */
    public static Map<Item, Integer> findExactCombination(Player player, int amount) {
        Map<Item, Integer> available = countInventoryMedals(player);
        Map<Item, Integer> toTake = new LinkedHashMap<>();

        int remaining = amount;
        for (Map.Entry<Item, Integer> entry : MEDAL_VALUES.entrySet()) {
            Item medal = entry.getKey();
            int value = entry.getValue();
            int have = available.getOrDefault(medal, 0);
            int need = Math.min(remaining / value, have);
            if (need > 0) {
                toTake.put(medal, need);
                remaining -= need * value;
            }
        }

        return remaining == 0 ? toTake : null;
    }

    /**
     * Removes exact medal combination from player inventory.
     */
    public static void removeFromInventory(Player player, Map<Item, Integer> toRemove) {
        for (Map.Entry<Item, Integer> entry : toRemove.entrySet()) {
            int toRemoveCount = entry.getValue();
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == entry.getKey() && toRemoveCount > 0) {
                    int take = Math.min(stack.getCount(), toRemoveCount);
                    stack.shrink(take);
                    toRemoveCount -= take;
                }
            }
        }
    }

    /**
     * Greedy breakdown of an amount into medal denominations.
     * Royal=100, Gold=25, Silver=10, Bronze=1
     */
    public static Map<Item, Integer> greedyBreakdown(int amount) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        int remaining = amount;
        for (Map.Entry<Item, Integer> entry : MEDAL_VALUES.entrySet()) {
            int count = remaining / entry.getValue();
            if (count > 0) {
                result.put(entry.getKey(), count);
                remaining -= count * entry.getValue();
            }
        }
        return result;
    }

    /**
     * Display name for a medal item.
     */
    public static String medalName(Item item) {
        if (item == ModItems.ROYAL_MEDAL.get())  return "Royal Medal";
        if (item == ModItems.GOLD_MEDAL.get())   return "Gold Medal";
        if (item == ModItems.SILVER_MEDAL.get()) return "Silver Medal";
        if (item == ModItems.BRONZE_MEDAL.get()) return "Bronze Medal";
        return item.toString();
    }
}