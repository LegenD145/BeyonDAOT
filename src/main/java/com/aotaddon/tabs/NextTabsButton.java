package com.aotaddon.tabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Overflow arrow button shown at the end of the visible tab row when there are more enabled
 * tabs than fit within the target panel's width. Clicking it cycles the row to the next page.
 */
public class NextTabsButton extends Button {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 22;

    private final int slotIndex;

    public NextTabsButton(int slotIndex, int panelLeft, int panelTop, Consumer<Button> onPress) {
        super(panelLeft + slotIndex * (WIDTH + 1), panelTop - HEIGHT, WIDTH, HEIGHT,
                Component.empty(), onPress::accept, DEFAULT_NARRATION);
        this.slotIndex = slotIndex;
    }

    public void updatePosition(int panelLeft, int panelTop) {
        this.setX(panelLeft + slotIndex * (WIDTH + 1));
        this.setY(panelTop - HEIGHT);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        // Vanilla-like button plate.
        int topLeft = isHoveredOrFocused() ? 0xFFE3E3E3 : 0xFFC6C6C6;
        int bottomRight = isHoveredOrFocused() ? 0xFF4D4D4D : 0xFF555555;
        int fill = isHoveredOrFocused() ? 0xFF9D9D9D : 0xFF8B8B8B;
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + h, topLeft);
        graphics.fill(x + w - 1, y, x + w, y + h, bottomRight);
        graphics.fill(x, y + h - 1, x + w, y + h, bottomRight);

        // Draw a clean "next" double-chevron icon instead of text.
        int iconColor = active ? 0xFFFFFFFF : 0xFF7A7A7A;
        int centerX = x + w / 2;
        int centerY = y + h / 2;
        drawChevronRight(graphics, centerX - 5, centerY - 4, iconColor);
        drawChevronRight(graphics, centerX + 1, centerY - 4, iconColor);
    }

    private static void drawChevronRight(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 1, x + 1, y + 8, color);
        graphics.fill(x + 1, y + 2, x + 2, y + 7, color);
        graphics.fill(x + 2, y + 3, x + 3, y + 6, color);
        graphics.fill(x + 3, y + 4, x + 4, y + 5, color);
    }
}
