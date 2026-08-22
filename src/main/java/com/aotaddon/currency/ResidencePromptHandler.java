package com.aotaddon.currency;

import com.aotaddon.family.FamilyEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

/** Prompts players without a residence to choose one on login. */
public final class ResidencePromptHandler {

    private ResidencePromptHandler() {}

    public static void onPlayerLogin(ServerPlayer player) {
        if (ResidenceData.hasResidence(player)) {
            FamilyEventHandler.grantStartingBonusIfEligible(player);
            return;
        }

        player.sendSystemMessage(Component.literal(
                "§6[Welcome] §fChoose where you live to receive your starting currency:"
        ).withStyle(ChatFormatting.GOLD));

        Component eldia = Component.literal("[Paradis / Eldia]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/residence eldia"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Medals currency — click to choose Paradis"))));

        Component marley = Component.literal("[Marley]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/residence marley"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Banknotes currency — click to choose Marley"))));

        player.sendSystemMessage(Component.literal("  ").append(eldia).append("  ").append(marley));
    }
}
