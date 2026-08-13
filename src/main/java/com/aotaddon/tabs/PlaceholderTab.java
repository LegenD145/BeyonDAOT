package com.aotaddon.tabs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A visible-but-inert tab for features that are announced but not built yet — clicking it does
 * nothing. Kept visually identical to real tabs (no greyed-out/locked treatment) per Bodi's call
 * that curiosity from an unmarked tab is the intended teaser effect.
 *
 * Construct one per upcoming feature rather than subclassing, e.g.:
 *   new PlaceholderTab(new ItemStack(Items.NETHERITE_CHESTPLATE), "Equipment (Coming Soon)")
 */
public class PlaceholderTab extends TabBase {

    private final ItemStack icon;
    private final Component tooltip;

    public PlaceholderTab(ItemStack icon, String tooltip) {
        this.icon = icon;
        this.tooltip = Component.literal(tooltip);
    }

    @Override
    public void registerOnScreens() {
        // Registered externally via TabsMenu.addTabToScreen(...) by whoever constructs this,
        // same as any other tab — kept out of this class so one PlaceholderTab instance can be
        // reused across multiple screens without hardcoding a screen list here.
    }

    @Override
    public void onClick(Player player) {
        // Intentionally inert.
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return false;
    }

    @Override
    public void renderIcon(GuiGraphics graphics, int x, int y, int width, int height) {
        int iconX = x + (width - 16) / 2;
        int iconY = y + (height - 16) / 2;
        graphics.renderItem(icon, iconX, iconY);
    }

    @Override
    public Component getTooltip() {
        return tooltip;
    }
}
