package com.aotaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders stacked, fading red reward-gain lines to the LEFT of the crosshair
 * (right-aligned text ending a fixed gap short of center), instead of below it.
 * Registered via: NeoForge.EVENT_BUS.register(new RewardPopupOverlay());
 * inside the Dist.CLIENT block in AotAddon's constructor.
 */
@OnlyIn(Dist.CLIENT)
public class RewardPopupOverlay {

    private static final int COLOR_RED = 0xEE3333;
    private static final int LINE_HEIGHT = 11;
    private static final int GAP_RIGHT_OF_CROSSHAIR = 20;
    private static final int FIRST_LINE_Y_OFFSET = -18; // roughly vertically centered on crosshair

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.screen != null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Left edge that all lines align against, sitting just right of the crosshair
        int leftEdgeX = screenW / 2 + GAP_RIGHT_OF_CROSSHAIR;
        int y = screenH / 2 + FIRST_LINE_Y_OFFSET;

        for (RewardPopupManager.ActiveGroupView group : RewardPopupManager.tickAndGetVisible()) {
            int alphaByte = Math.round(group.alpha() * 255f) << 24;
            int color = COLOR_RED | alphaByte;

            for (String line : group.lines()) {
                graphics.drawString(mc.font, line, leftEdgeX, y, color, false);
                y += LINE_HEIGHT;
            }
            y += 4;
        }
    }
}