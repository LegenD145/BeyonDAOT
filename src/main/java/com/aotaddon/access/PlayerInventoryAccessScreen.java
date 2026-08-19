package com.aotaddon.access;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PlayerInventoryAccessScreen extends AbstractContainerScreen<PlayerInventoryAccessMenu> {
    private static final int PANEL_LIGHT = 0xFFC6C6C6;
    private static final int PANEL_MID = 0xFF8B8B8B;
    private static final int EDGE_LIGHT = 0xFFFFFFFF;
    private static final int EDGE_DARK = 0xFF373737;
    private static final int TEXT_COLOR = 0xFF404040;

    public PlayerInventoryAccessScreen(PlayerInventoryAccessMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 8 + (4 * 18) + 26 + 24 + (4 * 18) + 8 + 4;
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        drawPanelFrame(graphics, x, y, this.imageWidth, this.imageHeight);

        // Section separators
        graphics.fill(x + 7, y + 79, x + this.imageWidth - 7, y + 80, EDGE_DARK);
        graphics.fill(x + 7, y + 80, x + this.imageWidth - 7, y + 81, EDGE_LIGHT);
        graphics.fill(x + 7, y + 157, x + this.imageWidth - 7, y + 158, EDGE_DARK);
        graphics.fill(x + 7, y + 158, x + this.imageWidth - 7, y + 159, EDGE_LIGHT);

        // Target armor + offhand slots
        for (int i = 0; i < 4; i++) {
            drawSlotBg(graphics, x + 8, y + 8 + i * 18);
        }
        drawSlotBg(graphics, x + 77, y + 62);

        // Target inventory + hotbar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(graphics, x + 8 + col * 18, y + 34 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(graphics, x + 8 + col * 18, y + 92);
        }

        // Viewer inventory + hotbar
        int viewerOffsetY = 130;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(graphics, x + 8 + col * 18, y + viewerOffsetY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(graphics, x + 8 + col * 18, y + viewerOffsetY + 58);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, TEXT_COLOR, false);
        graphics.drawString(this.font, "Target Inventory", 8, 84, TEXT_COLOR, false);
        graphics.drawString(this.font, "Your Inventory", 8, 162, TEXT_COLOR, false);
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
}