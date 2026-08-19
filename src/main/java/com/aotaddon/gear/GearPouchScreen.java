package com.aotaddon.gear;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GearPouchScreen extends AbstractContainerScreen<GearPouchMenu> {

    private static final int PANEL_LIGHT = 0xFFC6C6C6;
    private static final int PANEL_MID = 0xFF8B8B8B;
    private static final int EDGE_LIGHT = 0xFFFFFFFF;
    private static final int EDGE_DARK = 0xFF373737;
    private static final int LABEL_COLOR = 0xFF404040;

    // GUI size
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 120;

    public GearPouchScreen(GearPouchMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanelFrame(graphics, x, y, imageWidth, imageHeight);

        // Section labels
        graphics.drawString(font, this.title, x + 8, y + 6, LABEL_COLOR, false);
        graphics.drawString(font, "Blades",  x + 8,  y + 10, LABEL_COLOR, false);
        graphics.drawString(font, "Gas",     x + 8,  y + 64, LABEL_COLOR, false);
        graphics.drawString(font, "Spears",  x + 30, y + 64, LABEL_COLOR, false);
        graphics.drawString(font, "Hotbar",  x + 8,  y + 88, LABEL_COLOR, false);

        // Divider line between pouch and hotbar
        graphics.fill(x + 4, y + 84, x + imageWidth - 4, y + 85, EDGE_DARK);
        graphics.fill(x + 4, y + 85, x + imageWidth - 4, y + 86, EDGE_LIGHT);

        // Blade slots (2x4)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                drawSlotBg(graphics, x + 8 + col * 18, y + 18 + row * 18);
            }
        }
        // Gas + spears
        drawSlotBg(graphics, x + 8, y + 58);
        drawSlotBg(graphics, x + 30, y + 58);
        drawSlotBg(graphics, x + 52, y + 58);
        // Hotbar
        for (int col = 0; col < 9; col++) {
            drawSlotBg(graphics, x + 8 + col * 18, y + 90);
        }
    }

    private void drawPanelFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_LIGHT);
        graphics.fill(x, y, x + w, y + 1, EDGE_LIGHT);
        graphics.fill(x, y, x + 1, y + h, EDGE_LIGHT);
        graphics.fill(x + w - 1, y, x + w, y + h, EDGE_DARK);
        graphics.fill(x, y + h - 1, x + w, y + h, EDGE_DARK);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_LIGHT);
    }

    private void drawSlotBg(GuiGraphics graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 1, EDGE_DARK);
        graphics.fill(sx, sy + 1, sx + 1, sy + 17, EDGE_DARK);
        graphics.fill(sx, sy + 17, sx + 18, sy + 18, EDGE_LIGHT);
        graphics.fill(sx + 17, sy, sx + 18, sy + 17, EDGE_LIGHT);
        graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, PANEL_MID);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Intentionally empty — we draw labels in renderBg with absolute coords
    }
}