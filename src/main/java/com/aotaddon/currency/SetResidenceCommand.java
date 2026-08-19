package com.aotaddon.currency;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * /setresidence <player> eldia|marley|clear — op 2.
 * Overrides currency nation; clear restores DAOT bloodline defaults.
 */
public class SetResidenceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setresidence")
                        .requires(src -> src.hasPermission(2))
                        .then(
                                Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("eldia").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "eldia")))
                                        .then(Commands.literal("marley").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "marley")))
                                        .then(Commands.literal("clear").executes(ctx ->
                                                execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), "clear")))
                        )
        );
    }

    private static int execute(CommandSourceStack source, net.minecraft.server.level.ServerPlayer target, String residence) {
        ResidenceData.set(target, residence);
        String shown = "clear".equals(residence) ? "bloodline default" : residence;
        String targetName = target.getName().getString();

        try {
            net.minecraft.server.level.ServerPlayer sender = source.getPlayerOrException();
            sender.sendSystemMessage(Component.literal(
                    "§aSet §f" + targetName + "§a's residence to §e" + shown + "§a."
            ));
            target.sendSystemMessage(Component.literal(
                    "§aYour residence was set to §e" + shown + "§a."
            ).withStyle(ChatFormatting.GREEN));
        } catch (Exception ignored) {
            source.getServer().sendSystemMessage(Component.literal(
                    "[TitanRequiem] Set " + targetName + "'s residence to " + shown
            ));
        }

        return 1;
    }
}
