package com.aotaddon.family;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * Registers /setfamily <player> <family|clear>
 * Permission level 2 (op) required.
 */
public class SetFamilyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setfamily")
                        .requires(src -> src.hasPermission(2))
                        .then(
                                Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("helos").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "helos")))
                                        .then(Commands.literal("fritz").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "fritz")))
                                        .then(Commands.literal("yeager").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "yeager")))
                                        .then(Commands.literal("reiss").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "reiss")))
                                        .then(Commands.literal("clear").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "")))
                        )
        );
    }

    private static int execute(CommandSourceStack source, net.minecraft.server.level.ServerPlayer target, String family) {
        FamilyData.setFamily(target, family);
        if (!family.isEmpty()) {
            FamilyEventHandler.sendFamilyMessage(target, family);
        } else {
            target.sendSystemMessage(Component.literal("§eYour family was cleared.").withStyle(ChatFormatting.YELLOW));
        }

        String targetName = target.getName().getString();
        String displayFamily = family.isEmpty() ? "clear" : family;
        try {
            net.minecraft.server.level.ServerPlayer sender = source.getPlayerOrException();
            sender.sendSystemMessage(Component.literal(
                    "§aSet §f" + targetName + "§a's family to §e" + displayFamily + "§a."
            ));
        } catch (Exception ignored) {
            source.getServer().sendSystemMessage(Component.literal(
                    "[TitanRequiem] Set " + targetName + "'s family to " + displayFamily
            ));
        }

        return 1;
    }
}