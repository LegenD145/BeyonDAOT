package com.aotaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders a red "⚔ COMBAT Xs" indicator below the crosshair when the player
 * is combat tagged.
 */
@OnlyIn(Dist.CLIENT)
public class CombatTagOverlay {

    private static final int COLOR = 0xFFFF3333;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientCombatTagState.isInCombat()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int seconds = ClientCombatTagState.getSecondsLeft();
        Component text = Component.literal("\u2694 COMBAT " + seconds + "s");

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = (screenW - mc.font.width(text)) / 2;
        int y = screenH / 2 + 16;

        graphics.drawString(mc.font, text, x, y, COLOR, true);
    }
}
