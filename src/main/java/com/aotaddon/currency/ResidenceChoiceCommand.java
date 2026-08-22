package com.aotaddon.currency;

import com.aotaddon.family.FamilyEventHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /residence eldia|marley — lets players pick their currency nation once.
 * Admins can still override with /setresidence.
 */
public final class ResidenceChoiceCommand {

    private ResidenceChoiceCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("residence")
                        .then(Commands.literal("eldia").executes(ctx -> choose(ctx.getSource(), "eldia")))
                        .then(Commands.literal("marley").executes(ctx -> choose(ctx.getSource(), "marley")))
        );
    }

    private static int choose(CommandSourceStack source, String residence) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        if (ResidenceData.hasResidence(player)) {
            source.sendFailure(Component.literal(
                    "You already chose " + ResidenceData.get(player) + ". Ask an admin to change it."
            ).withStyle(ChatFormatting.RED));
            return 0;
        }

        ResidenceData.set(player, residence);
        String label = "eldia".equals(residence) ? "Paradis (medals)" : "Marley (banknotes)";
        player.sendSystemMessage(Component.literal(
                "§a[Residence] §fYou now live in §e" + label + "§f."
        ).withStyle(ChatFormatting.GREEN));

        FamilyEventHandler.grantStartingBonusIfEligible(player);
        return 1;
    }
}
