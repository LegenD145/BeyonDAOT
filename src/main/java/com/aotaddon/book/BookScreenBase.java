package com.aotaddon.client.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared plumbing for BookCoverScreen and BookOpenScreen: both need the same
 * 5 sticky tabs, evenly spaced, clickable, at the same relative position
 * along whatever panel is currently showing (cover, or the open spread).
 *
 * Layout rule (per Alex): top row (Stats/Talents/Settings) evenly spaced
 * across the panel width, poking above the top edge. Bottom row
 * (Reputation/[unassigned]) evenly spaced within the right half of the
 * panel width, poking below the bottom edge.
 */
public abstract class BookScreenBase extends Screen {

    // Native tab art dimensions (all 5 colors share this exact shape).
    protected static final int TAB_NATIVE_W = 51;
    protected static final int TAB_NATIVE_H = 26;

    // Display size for tabs - kept smaller than a straight 1:1 scale of the
    // panel so 3 tabs comfortably fit across the top edge without crowding.
    // Shrunk from 64 -> 44 per Alex's "book should be smaller, tabs included" note.
    protected static final int TAB_DISPLAY_W = 44;
    protected static final int TAB_DISPLAY_H = TAB_DISPLAY_W * TAB_NATIVE_H / TAB_NATIVE_W;

    private final List<TabHitbox> hitboxes = new ArrayList<>();

    protected BookScreenBase(Component title) {
        super(title);
    }

    /**
     * Call once per render() before drawing tab textures, to rebuild hitboxes
     * for the current frame's layout (panel may reposition if window resizes).
     *
     * @param panelLeft  left edge x of the cover or open-spread panel
     * @param panelTop   top edge y of the panel
     * @param panelWidth total width of the panel (single cover, or both pages combined)
     */
    protected void layoutTabs(int panelLeft, int panelTop, int panelWidth, int panelBottom) {
        hitboxes.clear();

        List<BookTab> left = new ArrayList<>();
        List<BookTab> stacked = new ArrayList<>();
        for (BookTab tab : BookTab.values()) {
            (tab.row() == BookTab.TabGroup.LEFT_SPREAD ? left : stacked).add(tab);
        }

        // All 5 tabs live along the TOP edge only, poking above panelTop.
        // Slots: one per left-group tab, plus ONE slot (at the right end)
        // shared by the stacked pair - they occupy the same horizontal
        // position, just offset vertically.
        int slotCount = left.size() + (stacked.isEmpty() ? 0 : 1);
        int[] slotX = evenlySpaced(panelLeft, panelWidth, slotCount, TAB_DISPLAY_W);
        int topY = panelTop - (TAB_DISPLAY_H / 2);

        for (int i = 0; i < left.size(); i++) {
            hitboxes.add(new TabHitbox(left.get(i), slotX[i], topY, TAB_DISPLAY_W, TAB_DISPLAY_H));
        }

        if (!stacked.isEmpty()) {
            int stackX = slotX[slotX.length - 1];
            int stackGap = 3;
            int y = topY;
            for (BookTab tab : stacked) {
                hitboxes.add(new TabHitbox(tab, stackX, y, TAB_DISPLAY_W, TAB_DISPLAY_H));
                y += TAB_DISPLAY_H + stackGap;
            }
        }
    }

    /** Even margins before/between/after N items of the given size inside a container span. */
    private int[] evenlySpaced(int containerStart, int containerSize, int count, int itemSize) {
        if (count == 0) return new int[0];
        int gap = (containerSize - count * itemSize) / (count + 1);
        int[] result = new int[count];
        int pos = containerStart + gap;
        for (int i = 0; i < count; i++) {
            result[i] = pos;
            pos += itemSize + gap;
        }
        return result;
    }

    /** Draws all 5 tabs at their laid-out positions. Call after layoutTabs() each frame. */
    protected void renderTabs(GuiGraphics gfx) {
        for (TabHitbox hb : hitboxes) {
            gfx.blit(hb.tab.texture(), hb.x, hb.y, hb.w, hb.h, 0, 0, TAB_NATIVE_W, TAB_NATIVE_H, TAB_NATIVE_W, TAB_NATIVE_H);
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

    private record TabHitbox(BookTab tab, int x, int y, int w, int h) {}
}