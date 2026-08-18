package com.aotaddon.command;

import com.aotaddon.config.AddonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * In-game control for campfire sit heal speed.
 * /campfireheal          — show current seconds-to-full
 * /campfireheal <1-600>  — set seconds to go from 0 HP to full while sitting
 * /campfireheal 0        — disable sit healing
 */
public final class CampfireHealCommand {

    private CampfireHealCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("campfireheal")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> show(ctx.getSource()))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                                .executes(ctx -> set(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "seconds"))))
        );
    }

    private static int show(CommandSourceStack source) {
        int seconds = AddonConfig.CAMPFIRE_SIT_FULL_HEAL_SECONDS.get();
        source.sendSuccess(() -> Component.literal(statusText(seconds)).withStyle(ChatFormatting.GREEN), false);
        return seconds;
    }

    private static int set(CommandSourceStack source, int seconds) {
        AddonConfig.CAMPFIRE_SIT_FULL_HEAL_SECONDS.set(seconds);
        source.sendSuccess(() -> Component.literal(statusText(seconds)).withStyle(ChatFormatting.GREEN), true);
        return seconds;
    }

    private static String statusText(int seconds) {
        if (seconds <= 0) {
            return "Campfire sit heal is off. /campfireheal <seconds> to enable.";
        }
        return "Campfire sit heal: empty to full in " + seconds + " seconds. Change with /campfireheal <seconds>.";
    }
}
