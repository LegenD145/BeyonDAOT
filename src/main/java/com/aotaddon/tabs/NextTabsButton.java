package com.aotaddon.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Overflow arrow button shown at the end of the visible tab row when there are more enabled
 * tabs than fit within the target panel's width. Clicking it cycles the row to the next page.
 *
 * TODO: swap the placeholder ">>" text render for an actual arrow icon texture once one exists.
 */
public class NextTabsButton extends Button {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 22;

    private final int slotIndex;

    public NextTabsButton(int slotIndex, int panelLeft, int panelTop, Consumer<Button> onPress) {
        super(panelLeft + slotIndex * (WIDTH + 1), panelTop - HEIGHT, WIDTH, HEIGHT,
                Component.literal(">>"), onPress::accept, DEFAULT_NARRATION);
        this.slotIndex = slotIndex;
    }

    public void updatePosition(int panelLeft, int panelTop) {
        this.setX(panelLeft + slotIndex * (WIDTH + 1));
        this.setY(panelTop - HEIGHT);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(Minecraft.getInstance().font, ">>",
                getX() + width / 2, getY() + (height - 8) / 2, 0xFFFFFF);
    }
}
