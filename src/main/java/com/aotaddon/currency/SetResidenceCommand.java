package com.aotaddon.currency;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                Commands.argument("player", StringArgumentType.word())
                                        .then(Commands.literal("eldia").executes(ctx ->
                                                execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "eldia")))
                                        .then(Commands.literal("marley").executes(ctx ->
                                                execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "marley")))
                                        .then(Commands.literal("clear").executes(ctx ->
                                                execute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "clear")))
                        )
        );
    }

    private static int execute(CommandSourceStack source, String targetName, String residence) {
        net.minecraft.server.level.ServerPlayer target = null;
        for (net.minecraft.server.level.ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equals(targetName)) {
                target = p;
                break;
            }
        }

        if (target == null) {
            source.sendFailure(Component.literal("Player not found or not online: " + targetName)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ResidenceData.set(target, residence);
        String shown = "clear".equals(residence) ? "bloodline default" : residence;

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
