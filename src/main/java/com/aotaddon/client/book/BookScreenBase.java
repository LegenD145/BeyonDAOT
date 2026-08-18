package com.aotaddon.client.book;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared plumbing for BookCoverScreen and BookOpenScreen.
 *
 * Tabs sit on the top and bottom edges, standing vertically:
 * red / yellow / black clustered top-left, green / blue clustered bottom-right.
 * Native flag art is landscape; ±90° rotation makes them poke out of the edge.
 */
public abstract class BookScreenBase extends Screen {

    protected static final int TAB_NATIVE_W = 51;
    protected static final int TAB_NATIVE_H = 26;

    /** Long side of the flag (pokes out of the book). */
    protected static final int TAB_POKE = 32;
    /** Short side of the flag (runs along the book edge). */
    protected static final int TAB_ALONG = TAB_POKE * TAB_NATIVE_H / TAB_NATIVE_W;

    private static final int EDGE_INSET = 8;
    /** 0.75 of one-tab preview gap. */
    private static final int TAB_GAP = TAB_ALONG * 3 / 4;

    private final List<TabHitbox> hitboxes = new ArrayList<>();

    protected BookScreenBase(Component title) {
        super(title);
    }

    /**
     * Call once per render() before drawing tab textures, to rebuild hitboxes
     * for the current frame's layout (panel may reposition if window resizes).
     */
    protected void layoutTabs(int panelLeft, int panelTop, int panelWidth, int panelBottom) {
        hitboxes.clear();

        List<BookTab> top = new ArrayList<>();
        List<BookTab> bottom = new ArrayList<>();
        for (BookTab tab : BookTab.values()) {
            (tab.row() == BookTab.TabGroup.TOP ? top : bottom).add(tab);
        }

        // Attach end sits on the visual rim. Body pokes out. No inset, no air gap.
        int topY = panelTop - TAB_POKE + 2;
        int x = panelLeft + EDGE_INSET;
        for (BookTab tab : top) {
            hitboxes.add(new TabHitbox(tab, x, topY, TAB_ALONG, TAB_POKE, 90));
            x += TAB_ALONG + TAB_GAP;
        }

        int bottomY = panelBottom;
        int bottomWidth = bottom.size() * TAB_ALONG + Math.max(0, bottom.size() - 1) * TAB_GAP;
        x = panelLeft + panelWidth - EDGE_INSET - bottomWidth;
        for (BookTab tab : bottom) {
            hitboxes.add(new TabHitbox(tab, x, bottomY, TAB_ALONG, TAB_POKE, 270));
            x += TAB_ALONG + TAB_GAP;
        }
    }

    /**
     * Draws tabs standing on the edge. Blits the landscape texture then rotates
     * so the swallowtail pokes out. Top = 90°, bottom = 270° (180° from prior).
     */
    protected void renderTabs(GuiGraphics gfx) {
        for (TabHitbox hb : hitboxes) {
            gfx.pose().pushPose();
            gfx.pose().translate(hb.x + hb.w / 2.0, hb.y + hb.h / 2.0, 0);
            gfx.pose().mulPose(Axis.ZP.rotationDegrees(hb.rotation));
            gfx.pose().translate(-TAB_POKE / 2.0, -TAB_ALONG / 2.0, 0);
            gfx.blit(hb.tab.texture(), 0, 0, TAB_POKE, TAB_ALONG,
                    0, 0, TAB_NATIVE_W, TAB_NATIVE_H, TAB_NATIVE_W, TAB_NATIVE_H);
            gfx.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (TabHitbox hb : hitboxes) {
                if (mouseX >= hb.x && mouseX < hb.x + hb.w && mouseY >= hb.y && mouseY < hb.y + hb.h) {
                    onTabClicked(hb.tab);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected abstract void onTabClicked(BookTab tab);

    protected static void playPageTurnSound() {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record TabHitbox(BookTab tab, int x, int y, int w, int h, int rotation) {}
}
