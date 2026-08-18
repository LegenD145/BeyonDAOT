package com.aotaddon.currency;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Routes /balance to medals (Eldian) or banknotes (Marley) by DAOT bloodline.
 */
final class FactionWallet {

    final String noun;
    final Function<ServerPlayer, Integer> getBalance;
    final BiConsumer<ServerPlayer, Integer> addBalance;
    final BiFunction<ServerPlayer, Integer, Map<Item, Integer>> findExact;
    final BiConsumer<ServerPlayer, Map<Item, Integer>> removeFromInventory;
    final Function<Integer, Map<Item, Integer>> greedyBreakdown;
    final Function<Item, String> itemName;
    final BiPredicate<ServerPlayer, Integer> deduct;
    final boolean usesMedalStock;

    private FactionWallet(
            String noun,
            Function<ServerPlayer, Integer> getBalance,
            BiConsumer<ServerPlayer, Integer> addBalance,
            BiFunction<ServerPlayer, Integer, Map<Item, Integer>> findExact,
            BiConsumer<ServerPlayer, Map<Item, Integer>> removeFromInventory,
            Function<Integer, Map<Item, Integer>> greedyBreakdown,
            Function<Item, String> itemName,
            BiPredicate<ServerPlayer, Integer> deduct,
            boolean usesMedalStock
    ) {
        this.noun = noun;
        this.getBalance = getBalance;
        this.addBalance = addBalance;
        this.findExact = findExact;
        this.removeFromInventory = removeFromInventory;
        this.greedyBreakdown = greedyBreakdown;
        this.itemName = itemName;
        this.deduct = deduct;
        this.usesMedalStock = usesMedalStock;
    }

    static FactionWallet of(ServerPlayer player) {
        return switch (CurrencyFaction.get(player)) {
            case ELDIAN -> new FactionWallet(
                    "medals",
                    MedalData::getBalance,
                    MedalData::addBalance,
                    MedalHelper::findExactCombination,
                    MedalHelper::removeFromInventory,
                    MedalHelper::greedyBreakdown,
                    MedalHelper::medalName,
                    MedalData::deductBalance,
                    true
            );
            case MARLEY -> new FactionWallet(
                    "banknotes",
                    BanknoteData::getBalance,
                    BanknoteData::addBalance,
                    BanknoteHelper::findExactCombination,
                    BanknoteHelper::removeFromInventory,
                    BanknoteHelper::greedyBreakdown,
                    BanknoteHelper::banknoteName,
                    BanknoteData::deductBalance,
                    false
            );
            case NONE -> null;
        };
    }

    static int failNoFaction(CommandSourceStack source) {
        source.sendFailure(Component.literal(
                "You have no faction currency. Set bloodline to eldian or marley.")
                .withStyle(ChatFormatting.RED));
        return 0;
    }
}
