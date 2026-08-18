package com.aotaddon.client.book;

import com.aotaddon.AotAddon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The 5 sticky tabs. Top-left cluster (red, yellow, black) sits on the top
 * edge; bottom-right cluster (green, blue) hangs off the bottom edge.
 */
public enum BookTab {

    STATS("red_tab", "titanreqiuem.book.tab.stats", TabGroup.TOP),
    SETTINGS("yellow_tab", "titanreqiuem.book.tab.settings", TabGroup.TOP),
    TALENTS("black_tab", "titanreqiuem.book.tab.talents", TabGroup.TOP),
    UNASSIGNED("green_tab", "titanreqiuem.book.tab.unassigned", TabGroup.BOTTOM),
    REPUTATION("blue_tab", "titanreqiuem.book.tab.reputation", TabGroup.BOTTOM);

    public enum TabGroup { TOP, BOTTOM }

    private final ResourceLocation texture;
    private final Component label;
    private final TabGroup row;

    BookTab(String textureName, String labelKey, TabGroup row) {
        this.texture = ResourceLocation.fromNamespaceAndPath(
                AotAddon.MOD_ID, "textures/gui/" + textureName + ".png");
        this.label = Component.translatable(labelKey);
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
