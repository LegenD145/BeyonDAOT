package com.aotaddon.client;

import com.aotaddon.AotAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Bottom-left persistent HUD: icon + number for each currency, one row per
 * currency, stacked upward from the bottom-left corner. Format is always
 * "[icon] [number]" with no digit cap (plain toString on the number, no
 * truncation/rounding), per the format you specified.
 *
 * Register via: NeoForge.EVENT_BUS.register(new CurrencyHudOverlay());
 * inside the Dist.CLIENT block in AotAddon's constructor.
 *
 * To add Medals/Banknotes once you have an icon:
 *   1. Drop the texture at assets/aotaddon/textures/gui/<name>.png
 *   2. Add a ResourceLocation constant below like HONOR_ICON
 *   3. Add one more drawRow(...) call in onRenderGui, same pattern as honor
 */
@OnlyIn(Dist.CLIENT)
public class CurrencyHudOverlay {

    private static final ResourceLocation HONOR_ICON =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/gui/honor_points.png");
    private static final ResourceLocation CURRENCY_ICON =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/gui/currency.png");

    private static final int ICON_SIZE = 32;      // rendered 1:1 with the source texture — no GPU scaling, no blur
    private static final int TEXTURE_SIZE = 32;
    private static final int ROW_HEIGHT = 20;
    private static final int MARGIN_RIGHT = 8;
    private static final int MARGIN_Y = 8;
    private static final int ICON_TEXT_GAP = 5;
    private static final int TEXT_COLOR = 0xFFFFFF;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Bottom row (closest to corner): Honor Points
        int y = screenH - MARGIN_Y - ICON_SIZE;
        String honorText = formatBalance(ClientHonorData.getBalance());
        int rowWidth = ICON_SIZE + ICON_TEXT_GAP + mc.font.width(honorText);
        int x = screenW - MARGIN_RIGHT - rowWidth;
        drawRow(graphics, mc, HONOR_ICON, honorText, x, y);

        // Second row (above Honor): medals or banknotes from DAOT bloodline.
        int y2 = y - ROW_HEIGHT;
        String currencyText = formatBalance(ClientCurrencyState.getBalance());
        int rowWidth2 = ICON_SIZE + ICON_TEXT_GAP + mc.font.width(currencyText);
        int x2 = screenW - MARGIN_RIGHT - rowWidth2;
        drawRow(graphics, mc, CURRENCY_ICON, currencyText, x2, y2);
    }

    private void drawRow(GuiGraphics graphics, Minecraft mc, ResourceLocation icon, String text, int x, int y) {
        graphics.blit(icon, x, y, 0, 0, ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        int textX = x + ICON_SIZE + ICON_TEXT_GAP;
        int textY = y + (ICON_SIZE - mc.font.lineHeight) / 2;
        graphics.drawString(mc.font, text, textX, textY, TEXT_COLOR, true);
    }

    /**
     * No digit cap — shows the raw value. Honor Points can be fractional
     * (0.35, 0.25 per-kill rates), so whole numbers display clean (no ".0")
     * while fractional balances still show their decimal.
     */
    private static String formatBalance(double value) {
        return (value == Math.floor(value)) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Currency balances are plain ints (no decimals) — shown as-is, no cap. */
    private static String formatBalance(int value) {
        return String.valueOf(value);
    }
}