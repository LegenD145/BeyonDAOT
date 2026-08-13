package com.aotaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;
import java.util.Map;

/**
 * Right-side HUD showing active skill cooldowns (Impulse, Dodge, future
 * skills), styled after vanilla's subtitle overlay: a black bar per entry,
 * "> Label   X.Xs" text, stacked vertically. Longest remaining cooldown on
 * top, shortest on bottom - matches the mockup ordering (ODM Boost 12.4s
 * above Thunder Spear 5.2s above Blade Swap 1.8s).
 *
 * Each row's timer updates every frame (computed fresh from wall clock via
 * SkillCooldownTracker, not per-tick) and the row is removed entirely once
 * its cooldown hits 0, rather than showing "0.0s" - keeps the display from
 * bloating with finished cooldowns.
 *
 * Register via: NeoForge.EVENT_BUS.register(new SkillCooldownOverlay());
 * inside the Dist.CLIENT block in AotAddon's constructor, same as
 * CurrencyHudOverlay.
 */
@OnlyIn(Dist.CLIENT)
public class SkillCooldownOverlay {

    private static final int BAR_HEIGHT = 18;
    private static final int BAR_PADDING_X = 6;
    private static final int ROW_GAP = 2;
    private static final int MARGIN_RIGHT = 8;
    private static final int MARGIN_TOP = 40;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int BAR_COLOR = 0x90000000; // 56% opaque black, matches vanilla subtitle bg
    private static final String TIMER_GAP = "   "; // spacing between label and timer, per your mockup

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        List<Map.Entry<String, Float>> active = SkillCooldownTracker.getActiveSorted();
        if (active.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();

        int y = MARGIN_TOP;
        for (Map.Entry<String, Float> entry : active) {
            String text = "> " + entry.getKey() + TIMER_GAP + formatSeconds(entry.getValue());
            int textWidth = mc.font.width(text);
            int barWidth = textWidth + BAR_PADDING_X * 2;
            int x = screenW - MARGIN_RIGHT - barWidth;

            graphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, BAR_COLOR);

            int textX = x + BAR_PADDING_X;
            int textY = y + (BAR_HEIGHT - mc.font.lineHeight) / 2;
            graphics.drawString(mc.font, text, textX, textY, TEXT_COLOR, true);

            y += BAR_HEIGHT + ROW_GAP;
        }
    }

    private static String formatSeconds(float seconds) {
        float clamped = Math.max(0f, seconds);
        return String.format("%.1fs", clamped);
    }
}