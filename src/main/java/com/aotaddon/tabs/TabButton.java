package com.aotaddon.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Delegates the actual box/shading to vanilla's own Button rendering (the same widget sprite
 * used for every button in every vanilla menu screen), then draws the registered TabBase's icon
 * on top. This intentionally does NOT attempt to reproduce the rounded-top notched "tab" shape
 * from tab_menu_buttons.png in the reference screenshot — that requires either a real texture
 * asset or the exact vanilla tab-widget sprite IDs verified in-game, neither of which I can
 * confirm from source code alone in this environment. What you get instead is a plain vanilla
 * button box with correct shading/hover/selected-tint — guaranteed to render, unlike the
 * previous version's guessed sprite paths.
 */
public class TabButton extends Button {

    private static final int WIDTH = 26;
    private static final int HEIGHT = 22;

    private final int slotIndex;
    private final Class<? extends Screen> currentScreenClass;
    private TabBase tab;

    public TabButton(TabBase tab, int slotIndex, int panelLeft, int panelTop,
                     Class<? extends Screen> currentScreenClass) {
        super(panelLeft + slotIndex * (WIDTH + 1), panelTop - HEIGHT, WIDTH, HEIGHT,
                Component.empty(), b -> {
                }, DEFAULT_NARRATION);
        this.tab = tab;
        this.slotIndex = slotIndex;
        this.currentScreenClass = currentScreenClass;
        this.setTooltip(Tooltip.create(tab.getTooltip()));
    }

    public void setTab(TabBase tab) {
        this.tab = tab;
        this.setTooltip(Tooltip.create(tab.getTooltip()));
    }

    public void updatePosition(int panelLeft, int panelTop) {
        this.setX(panelLeft + slotIndex * (WIDTH + 1));
        this.setY(panelTop - HEIGHT);
    }

    @Override
    public void onPress() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            tab.onClick(player);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Real vanilla button background/shading/hover-highlight — same sprite path every
        // vanilla menu button uses, so this is guaranteed to render correctly.
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        boolean selected = tab.isCurrentlyActive(currentScreenClass);
        if (selected) {
            // Simple selected-state tint since vanilla Button has no built-in "selected" look.
            graphics.fill(getX() + 1, getY() + 1, getX() + WIDTH - 1, getY() + HEIGHT - 1,
                    0x40FFFFFF);
        }

        int iconMargin = 3;
        tab.renderIcon(graphics, getX() + iconMargin, getY() + iconMargin,
                WIDTH - iconMargin * 2, HEIGHT - iconMargin * 2);
    }
}