package com.aotaddon.tabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * A single tab in the inventory tab bar. Each concrete tab registers itself onto whichever
 * screen classes it should appear on (via {@link TabsMenu#addTabToScreen}) and knows how to
 * open its own target screen when clicked.
 */
public abstract class TabBase {

    /** Called once at client setup — register this tab onto every screen class it should appear on. */
    public abstract void registerOnScreens();

    /** Called when the player clicks this tab's button. */
    public abstract void onClick(Player player);

    /**
     * Whether this tab should currently be shown at all. Used e.g. by the Sophisticated Backpacks
     * tab to hide itself if the player has nothing in their Curios backpack slot, or by the
     * placeholder tabs if you ever want to gate them behind a permission/date.
     */
    public boolean isEnabled(Player player) {
        return true;
    }

    /**
     * Whether this tab represents the screen currently open (so TabButton can draw it as
     * "selected" rather than just "hovered"). Compare against the screen class, not an instance.
     */
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return false;
    }

    /**
     * Draw ONLY this tab's icon, centered within the icon area TabButton hands you (already
     * accounts for the vanilla tab sprite's border). Do not draw a background/border here —
     * TabButton draws the vanilla tab sprite chrome itself.
     */
    public abstract void renderIcon(GuiGraphics graphics, int x, int y, int width, int height);

    /** Tooltip shown on hover. */
    public abstract Component getTooltip();
}
