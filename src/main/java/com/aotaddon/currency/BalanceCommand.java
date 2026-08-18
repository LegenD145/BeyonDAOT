package com.aotaddon.currency;

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
        FactionWallet wallet = FactionWallet.of(target);
        if (wallet == null) {
            source.sendFailure(Component.literal(
                    target.getName().getString() + " has no faction currency. Set bloodline to eldian or marley.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        wallet.addBalance.accept(target, amount);
        source.sendSuccess(() -> Component.literal(
                        "§aAdded §f" + amount + "§a " + wallet.noun + " to §f" + target.getName().getString() + "§a's balance.")
                .withStyle(ChatFormatting.GREEN), true);
        target.sendSystemMessage(Component.literal(
                        "§aAn admin has added §f" + amount + "§a " + wallet.noun + " to your balance.")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")

                .executes(ctx -> showBalance(ctx.getSource()))
                .then(Commands.literal("add")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addBalance(ctx.getSource(),
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))

                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> deposit(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))

                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> withdraw(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))

                .then(Commands.literal("system")
                        .then(Commands.literal("info")
                                .executes(ctx -> systemInfo(ctx.getSource()))))
        );
    }

    private static int showBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        FactionWallet wallet = FactionWallet.of(player);
        if (wallet == null) {
            return FactionWallet.failNoFaction(source);
        }

        int balance = wallet.getBalance.apply(player);
        Map<Item, Integer> breakdown = wallet.greedyBreakdown.apply(balance);

        player.sendSystemMessage(Component.literal(
                "§6━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
                "§e  Balance: §f" + balance + " " + wallet.noun).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal(
                "§7  Breakdown:").withStyle(ChatFormatting.GRAY));

        for (Map.Entry<Item, Integer> entry : breakdown.entrySet()) {
            if (entry.getValue() > 0) {
                player.sendSystemMessage(Component.literal(
                        "§7    " + wallet.itemName.apply(entry.getKey()) +
                                " x" + entry.getValue()).withStyle(ChatFormatting.GRAY));
            }
        }

        player.sendSystemMessage(Component.literal(
                "§6━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int deposit(CommandSourceStack source, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        FactionWallet wallet = FactionWallet.of(player);
        if (wallet == null) {
            return FactionWallet.failNoFaction(source);
        }

        Map<Item, Integer> toTake = wallet.findExact.apply(player, amount);
        if (toTake == null) {
            player.sendSystemMessage(Component.literal(
                    "§cYour " + wallet.noun + " are not enough to be transferred.").withStyle(ChatFormatting.RED));
            return 0;
        }

        wallet.removeFromInventory.accept(player, toTake);
        wallet.addBalance.accept(player, amount);

        if (wallet.usesMedalStock) {
            SystemStockData stock = SystemStockData.get(player.getServer());
            stock.addMedals(toTake);
        }

        player.sendSystemMessage(Component.literal(
                "§aDeposited §f" + amount + "§a " + wallet.noun + " into your balance.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int withdraw(CommandSourceStack source, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        FactionWallet wallet = FactionWallet.of(player);
        if (wallet == null) {
            return FactionWallet.failNoFaction(source);
        }

        if (!wallet.deduct.test(player, amount)) {
            player.sendSystemMessage(Component.literal(
                            "§cYou don't have enough balance to withdraw §f" + amount + "§c " + wallet.noun + ".")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        Map<Item, Integer> toGive = wallet.greedyBreakdown.apply(amount);
        for (Map.Entry<Item, Integer> entry : toGive.entrySet()) {
            if (entry.getValue() > 0) {
                ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }

        player.sendSystemMessage(Component.literal(
                "§aWithdrew §f" + amount + "§a " + wallet.noun + " from your balance.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

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
