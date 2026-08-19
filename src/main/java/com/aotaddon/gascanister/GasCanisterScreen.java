package com.aotaddon.gascanister;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GasCanisterScreen extends AbstractContainerScreen<GasCanisterMenu> {

    private static final int PANEL_LIGHT = 0xFFC6C6C6;
    private static final int PANEL_MID = 0xFF8B8B8B;
    private static final int EDGE_LIGHT = 0xFFFFFFFF;
    private static final int EDGE_DARK = 0xFF373737;
    private static final int TEXT_COLOR = 0xFF404040;

    private static final int FILL_SLOT_X = 25;
    private static final int FILL_SLOT_Y = 23;
    private static final int TAKE_SLOT_X = 25;
    private static final int TAKE_SLOT_Y = 55;
    private static final int INV_Y = 84;

    private static final int TANK_W = 16;
    private static final int TANK_H = 50;
    private static final int TANK_X = 134;
    private static final int TANK_Y = 18;

    public GasCanisterScreen(GasCanisterMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 86;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanelFrame(gfx, x, y, imageWidth, imageHeight);
        drawInnerCorners(gfx, x + 2, y + 2, imageWidth - 4, imageHeight - 4);

        // Subtle separator between machine area and player inventory.
        gfx.fill(x + 7, y + 74, x + imageWidth - 7, y + 75, EDGE_DARK);
        gfx.fill(x + 7, y + 75, x + imageWidth - 7, y + 76, EDGE_LIGHT);

        gfx.drawString(font, this.title, x + 8, y + 6, TEXT_COLOR, false);
        gfx.drawString(font, "Inventory", x + 8, y + INV_Y - 9, TEXT_COLOR, false);

        // Slot backgrounds.
        drawSlotBg(gfx, x + FILL_SLOT_X, y + FILL_SLOT_Y);
        drawSlotBg(gfx, x + TAKE_SLOT_X, y + TAKE_SLOT_Y);

        // Labels centered around each slot lane.
        gfx.drawString(font, "Fill", x + 9, y + FILL_SLOT_Y + 4, TEXT_COLOR, false);
        gfx.drawString(font, "Take", x + 7, y + TAKE_SLOT_Y + 4, TEXT_COLOR, false);

        // Arrows and lane bars.
        int arrowStartX = x + 50;
        drawArrowRight(gfx, arrowStartX, y + FILL_SLOT_Y + 4);
        drawArrowLeft(gfx, arrowStartX, y + TAKE_SLOT_Y + 4);
        gfx.fill(arrowStartX + 16, y + FILL_SLOT_Y + 7, x + TANK_X - 8, y + FILL_SLOT_Y + 9, TEXT_COLOR);
        gfx.fill(arrowStartX + 16, y + TAKE_SLOT_Y + 7, x + TANK_X - 8, y + TAKE_SLOT_Y + 9, TEXT_COLOR);

        // Tank visual shifted to the right.
        int tankX = x + TANK_X;
        int tankY = y + TANK_Y;
        drawSlotBg(gfx, tankX - 1, tankY - 1);
        gfx.fill(tankX, tankY, tankX + TANK_W, tankY + TANK_H, PANEL_MID);

        // Fill level.
        int stored = menu.getStoredGas();
        int fillH = (int) ((stored / (float) GasCanisterBlockEntity.MAX_STORED_GAS) * TANK_H);
        if (fillH > 0) {
            gfx.fill(tankX, tankY + TANK_H - fillH, tankX + TANK_W, tankY + TANK_H, 0xFF3399FF);
        }

        // Gas text below gauge.
        String gasText = stored + "/" + GasCanisterBlockEntity.MAX_STORED_GAS;
        int tw = font.width(gasText);
        gfx.drawString(font, gasText, tankX + (TANK_W - tw) / 2, tankY + TANK_H + 1, TEXT_COLOR, false);

        // Player inventory slot backgrounds.
        int invY = y + INV_Y;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gfx, x + 7 + col * 18, invY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gfx, x + 7 + col * 18, invY + 58);
        }
    }

    private void drawPanelFrame(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, PANEL_LIGHT);
        gfx.fill(x, y, x + w, y + 1, EDGE_LIGHT);
        gfx.fill(x, y, x + 1, y + h, EDGE_LIGHT);
        gfx.fill(x + w - 1, y, x + w, y + h, EDGE_DARK);
        gfx.fill(x, y + h - 1, x + w, y + h, EDGE_DARK);
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_LIGHT);
    }

    private void drawInnerCorners(GuiGraphics gfx, int x, int y, int w, int h) {
        // Extra vanilla-like panel corner accents.
        gfx.fill(x, y, x + 3, y + 1, EDGE_LIGHT);
        gfx.fill(x, y, x + 1, y + 3, EDGE_LIGHT);

        gfx.fill(x + w - 3, y, x + w, y + 1, EDGE_LIGHT);
        gfx.fill(x + w - 1, y, x + w, y + 3, EDGE_DARK);

        gfx.fill(x, y + h - 1, x + 3, y + h, EDGE_DARK);
        gfx.fill(x, y + h - 3, x + 1, y + h, EDGE_LIGHT);

        gfx.fill(x + w - 3, y + h - 1, x + w, y + h, EDGE_DARK);
        gfx.fill(x + w - 1, y + h - 3, x + w, y + h, EDGE_DARK);
    }

    private void drawSlotBg(GuiGraphics gfx, int sx, int sy) {
        // Top edge (dark)
        gfx.fill(sx, sy, sx + 18, sy + 1, EDGE_DARK);
        // Left edge (dark)
        gfx.fill(sx, sy + 1, sx + 1, sy + 17, EDGE_DARK);
        // Bottom edge (white highlight)
        gfx.fill(sx, sy + 17, sx + 18, sy + 18, EDGE_LIGHT);
        // Right edge (white highlight)
        gfx.fill(sx + 17, sy, sx + 18, sy + 17, EDGE_LIGHT);
        // Inner fill
        gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, PANEL_MID);
    }

    private void drawArrowRight(GuiGraphics gfx, int x, int y) {
        int c = 0xFF404040;
        gfx.fill(x, y + 3, x + 16, y + 5, c);
        gfx.fill(x + 12, y + 1, x + 13, y + 7, c);
        gfx.fill(x + 13, y + 2, x + 14, y + 6, c);
        gfx.fill(x + 14, y + 3, x + 15, y + 5, c);
    }

    private void drawArrowLeft(GuiGraphics gfx, int x, int y) {
        int c = 0xFF404040;
        gfx.fill(x + 2, y + 3, x + 18, y + 5, c);
        gfx.fill(x + 5, y + 1, x + 6, y + 7, c);
        gfx.fill(x + 4, y + 2, x + 5, y + 6, c);
        gfx.fill(x + 3, y + 3, x + 4, y + 5, c);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
