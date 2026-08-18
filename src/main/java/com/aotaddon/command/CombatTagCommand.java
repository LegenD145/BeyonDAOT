package com.aotaddon.command;

import com.aotaddon.config.AddonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * In-game control for combat tag duration.
 * /combattag           — show current seconds
 * /combattag <1-600>   — set tag length after taking damage
 * /combattag 0         — disable combat tagging
 */
public final class CombatTagCommand {

    private CombatTagCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("combattag")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> show(ctx.getSource()))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                                .executes(ctx -> set(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "seconds"))))
        );
    }

    private static int show(CommandSourceStack source) {
        int seconds = AddonConfig.COMBAT_TAG_SECONDS.get();
        source.sendSuccess(() -> Component.literal(statusText(seconds)).withStyle(ChatFormatting.GREEN), false);
        return seconds;
    }

    private static int set(CommandSourceStack source, int seconds) {
        AddonConfig.COMBAT_TAG_SECONDS.set(seconds);
        source.sendSuccess(() -> Component.literal(statusText(seconds)).withStyle(ChatFormatting.GREEN), true);
        return seconds;
    }

    private static String statusText(int seconds) {
        if (seconds <= 0) {
            return "Combat tag is off. /combattag <seconds> to enable.";
        }
        return "Combat tag lasts " + seconds + " seconds after damage. Change with /combattag <seconds>.";
    }
}
