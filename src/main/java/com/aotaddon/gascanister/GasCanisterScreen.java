package com.aotaddon.gascanister;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Plain rect+text GUI, same style as GearPouchScreen and daot's own StrwsScreen
 * (neither uses a custom texture PNG).
 */
public class GasCanisterScreen extends AbstractContainerScreen<GasCanisterMenu> {

    private static final int BG_COLOR = 0xCC1A1A1A;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int LABEL_COLOR = 0xFFAAAAAA;

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    public GasCanisterScreen(GasCanisterMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, BG_COLOR);
        graphics.fill(x, y, x + imageWidth, y + 1, BORDER_COLOR);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + imageHeight, BORDER_COLOR);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, BORDER_COLOR);

        graphics.drawString(font, "Gas Canister", x + 8, y + 6, LABEL_COLOR, false);
        graphics.drawString(font, "Insert canister:", x + 8, y + 24, LABEL_COLOR, false);

        int stored = menu.getStoredGas();
        graphics.drawString(font, "Stored: " + stored + "/" + GasCanisterBlockEntity.MAX_STORED_GAS,
                x + 8, y + 60, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Empty - labels drawn with absolute coords in renderBg, same as GearPouchScreen
    }
}
