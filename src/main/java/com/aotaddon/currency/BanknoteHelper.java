package com.aotaddon.currency;

import com.aotaddon.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Marley-side mirror of MedalHelper. Denominations descending order:
 * 100, 50, 20, 10, 5, 1 - real currency values, face value = medal value.
 */
public class BanknoteHelper {

    public static final LinkedHashMap<Item, Integer> BANKNOTE_VALUES = new LinkedHashMap<>();

    static {
        BANKNOTE_VALUES.put(ModItems.MARLEY_BANKNOTE_100.get(), 100);
        BANKNOTE_VALUES.put(ModItems.MARLEY_BANKNOTE_50.get(), 50);
        BANKNOTE_VALUES.put(ModItems.MARLEY_BANKNOTE_20.get(), 20);
        BANKNOTE_VALUES.put(ModItems.MARLEY_BANKNOTE_10.get(), 10);
        BANKNOTE_VALUES.put(ModItems.MARLEY_BANKNOTE_5.get(), 5);
        BANKNOTE_VALUES.put(ModItems.MARLEY_BANKNOTE_1.get(), 1);
    }

    public static int countInventoryValue(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            Integer val = BANKNOTE_VALUES.get(stack.getItem());
            if (val != null) total += val * stack.getCount();
        }
        return total;
    }

    public static Map<Item, Integer> countInventoryBanknotes(Player player) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Item note : BANKNOTE_VALUES.keySet()) counts.put(note, 0);
        for (ItemStack stack : player.getInventory().items) {
            Integer val = BANKNOTE_VALUES.get(stack.getItem());
            if (val != null) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    public static Map<Item, Integer> findExactCombination(Player player, int amount) {
        Map<Item, Integer> available = countInventoryBanknotes(player);
        Map<Item, Integer> toTake = new LinkedHashMap<>();

        int remaining = amount;
        for (Map.Entry<Item, Integer> entry : BANKNOTE_VALUES.entrySet()) {
            Item note = entry.getKey();
            int value = entry.getValue();
            int have = available.getOrDefault(note, 0);
            int need = Math.min(remaining / value, have);
            if (need > 0) {
                toTake.put(note, need);
                remaining -= need * value;
            }
        }

        return remaining == 0 ? toTake : null;
    }

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

    public static Map<Item, Integer> greedyBreakdown(int amount) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        int remaining = amount;
        for (Map.Entry<Item, Integer> entry : BANKNOTE_VALUES.entrySet()) {
            int count = remaining / entry.getValue();
            if (count > 0) {
                result.put(entry.getKey(), count);
                remaining -= count * entry.getValue();
            }
        }
        return result;
    }

    public static String banknoteName(Item item) {
        if (item == ModItems.MARLEY_BANKNOTE_100.get()) return "$100 Banknote";
        if (item == ModItems.MARLEY_BANKNOTE_50.get()) return "$50 Banknote";
        if (item == ModItems.MARLEY_BANKNOTE_20.get()) return "$20 Banknote";
        if (item == ModItems.MARLEY_BANKNOTE_10.get()) return "$10 Banknote";
        if (item == ModItems.MARLEY_BANKNOTE_5.get()) return "$5 Banknote";
        if (item == ModItems.MARLEY_BANKNOTE_1.get()) return "$1 Banknote";
        return item.toString();
    }
}