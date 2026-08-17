package com.aotaddon.client.book;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The 5 sticky tabs on the book. Top row (Stats/Talents/Settings) and
 * bottom-right row (Reputation/[unassigned]) per Alex's layout.
 * All right-page content is empty for this first pass - each tab just
 * swaps which blank right page is shown.
 */
public enum BookTab {

    STATS("red_tab", "Stats", TabGroup.LEFT_SPREAD),
    TALENTS("black_tab", "Talents", TabGroup.LEFT_SPREAD),
    SETTINGS("yellow_tab", "Settings", TabGroup.LEFT_SPREAD),
    REPUTATION("blue_tab", "Reputation", TabGroup.STACKED),
    UNASSIGNED("green_tab", "???", TabGroup.STACKED);

    /** All tabs sit along the top edge. LEFT_SPREAD tabs are evenly spaced
     *  left-to-right; STACKED tabs share the rightmost slot, one directly
     *  below the other. */
    public enum TabGroup { LEFT_SPREAD, STACKED }

    private final ResourceLocation texture;
    private final Component label;
    private final TabGroup row;

    BookTab(String textureName, String label, TabGroup row) {
        this.texture = ResourceLocation.fromNamespaceAndPath(
                "titanreqiuem", "textures/gui/" + textureName + ".png");
        this.label = Component.literal(label);
        this.row = row;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public Component label() {
        return label;
    }

    public TabGroup row() {
        return row;
    }
}