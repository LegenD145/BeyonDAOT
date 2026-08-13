package com.aotaddon.gear;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GearPouchScreen extends AbstractContainerScreen<GearPouchMenu> {

    // We draw a plain dark background — no custom texture needed to start.
    // You can add a texture PNG later at assets/titanreqiuem/textures/gui/gear_pouch.png
    private static final int BG_COLOR      = 0xCC1A1A1A;
    private static final int BORDER_COLOR  = 0xFF555555;
    private static final int LABEL_COLOR   = 0xFFAAAAAA;

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

        // Background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, BG_COLOR);

        // Border
        graphics.fill(x,                  y,                   x + imageWidth, y + 1,            BORDER_COLOR);
        graphics.fill(x,                  y + imageHeight - 1, x + imageWidth, y + imageHeight,  BORDER_COLOR);
        graphics.fill(x,                  y,                   x + 1,          y + imageHeight,  BORDER_COLOR);
        graphics.fill(x + imageWidth - 1, y,                   x + imageWidth, y + imageHeight,  BORDER_COLOR);

        // Section labels
        graphics.drawString(font, "Blades",  x + 8,  y + 8,  LABEL_COLOR, false);
        graphics.drawString(font, "Gas",     x + 8,  y + 64, LABEL_COLOR, false);
        graphics.drawString(font, "Spears",  x + 30, y + 64, LABEL_COLOR, false);
        graphics.drawString(font, "Hotbar",  x + 8,  y + 88, LABEL_COLOR, false);

        // Divider line between pouch and hotbar
        graphics.fill(x + 4, y + 84, x + imageWidth - 4, y + 85, BORDER_COLOR);
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