package com.aotaddon.currency;

import com.aotaddon.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class BalanceCommand {
    private static int addBalance(CommandSourceStack source, ServerPlayer target, int amount) {
        MedalData.addBalance(target, amount);
        source.sendSuccess(() -> Component.literal(
                        "§aAdded §f" + amount + "§a medals to §f" + target.getName().getString() + "§a's balance.")
                .withStyle(ChatFormatting.GREEN), true);
        target.sendSystemMessage(Component.literal(
                        "§aAn admin has added §f" + amount + "§a medals to your balance.")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")

                // /balance
                .executes(ctx -> showBalance(ctx.getSource()))
                // /balance add <player> <amount> — OP only
                .then(Commands.literal("add")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addBalance(ctx.getSource(),
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))

                // /balance deposit <amount>
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> deposit(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))

                // /balance withdraw <amount>
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> withdraw(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))

                // /balance system info
                .then(Commands.literal("system")
                        .then(Commands.literal("info")
                                .executes(ctx -> systemInfo(ctx.getSource()))))
        );
    }

    // =========================================================================
    // /balance
    // =========================================================================

    private static int showBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        int balance = MedalData.getBalance(player);
        Map<Item, Integer> breakdown = MedalHelper.greedyBreakdown(balance);

        player.sendSystemMessage(Component.literal(
                "§6━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
                "§e  Medal Balance: §f" + balance + " medals").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal(
                "§7  Breakdown:").withStyle(ChatFormatting.GRAY));

        for (Map.Entry<Item, Integer> entry : breakdown.entrySet()) {
            if (entry.getValue() > 0) {
                player.sendSystemMessage(Component.literal(
                        "§7    " + MedalHelper.medalName(entry.getKey()) +
                                " x" + entry.getValue()).withStyle(ChatFormatting.GRAY));
            }
        }

        player.sendSystemMessage(Component.literal(
                "§6━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GOLD));
        return 1;
    }

    // =========================================================================
    // /balance deposit <amount>
    // =========================================================================

    private static int deposit(CommandSourceStack source, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        Map<Item, Integer> toTake = MedalHelper.findExactCombination(player, amount);
        if (toTake == null) {
            player.sendSystemMessage(Component.literal(
                    "§cYour medals are not enough to be transferred.").withStyle(ChatFormatting.RED));
            return 0;
        }

        // Remove medals from inventory
        MedalHelper.removeFromInventory(player, toTake);

        // Add to player balance
        MedalData.addBalance(player, amount);

        // Add to system stock
        SystemStockData stock = SystemStockData.get(player.getServer());
        stock.addMedals(toTake);

        player.sendSystemMessage(Component.literal(
                "§aDeposited §f" + amount + "§a medals into your balance.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    // =========================================================================
    // /balance withdraw <amount>
    // =========================================================================

    private static int withdraw(CommandSourceStack source, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        if (!MedalData.deductBalance(player, amount)) {
            player.sendSystemMessage(Component.literal(
                            "§cYou don't have enough balance to withdraw §f" + amount + "§c medals.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Give medals using greedy breakdown
        Map<Item, Integer> toGive = MedalHelper.greedyBreakdown(amount);
        for (Map.Entry<Item, Integer> entry : toGive.entrySet()) {
            if (entry.getValue() > 0) {
                ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                if (!player.getInventory().add(stack)) {
                    // Inventory full — drop at player's feet
                    player.drop(stack, false);
                }
            }
        }

        player.sendSystemMessage(Component.literal(
                "§aWithdrew §f" + amount + "§a medals from your balance.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    // =========================================================================
    // /balance system info
    // =========================================================================

    private static int systemInfo(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        SystemStockData stock = SystemStockData.get(player.getServer());
        Map<Item, Integer> stockMap = stock.getStockMap();

        player.sendSystemMessage(Component.literal(
                "§6━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
                "§e  System Medal Stock").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal(
                "§7  Total Value: §f" + stock.getTotalValue() + " medals").withStyle(ChatFormatting.GRAY));

        for (Map.Entry<Item, Integer> entry : stockMap.entrySet()) {
            player.sendSystemMessage(Component.literal(
                    "§7    " + MedalHelper.medalName(entry.getKey()) +
                            ": §f" + entry.getValue()).withStyle(ChatFormatting.GRAY));
        }

        player.sendSystemMessage(Component.literal(
                "§6━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GOLD));
        return 1;
    }
}