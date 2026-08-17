package com.aotaddon.client.book;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * First screen shown when the book keybind is pressed: the closed cover
 * (Book.png) with all 5 sticky tabs visible and clickable right away.
 * Clicking any tab plays the page-turn sound and opens BookOpenScreen with
 * that tab pre-selected.
 */
public class BookCoverScreen extends BookScreenBase {

    private static final ResourceLocation COVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("titanreqiuem", "textures/gui/book.png");

    // Book.png is a 230x162 canvas; actual cover art occupies a smaller
    // region within it (bbox measured directly from the re-exported file -
    // rest is transparent padding). Updated for the latest export: content
    // now runs full canvas height (v=0..162), padded only left/right.
    private static final int COVER_SRC_U = 65;
    private static final int COVER_SRC_V = 0;
    private static final int COVER_SRC_W = 98;
    private static final int COVER_SRC_H = 162;
    private static final int COVER_TEX_W = 230;
    private static final int COVER_TEX_H = 162;

    // Shrunk from 3 -> 2 per Alex's "book.png should be smaller" note.
    private static final int DISPLAY_SCALE = 2;
    private static final int COVER_DISPLAY_W = COVER_SRC_W * DISPLAY_SCALE;
    private static final int COVER_DISPLAY_H = COVER_SRC_H * DISPLAY_SCALE;

    private int coverLeft, coverTop;

    public BookCoverScreen() {
        super(Component.literal("Book"));
    }

    @Override
    protected void init() {
        super.init();
        this.coverLeft = (this.width - COVER_DISPLAY_W) / 2;
        this.coverTop = (this.height - COVER_DISPLAY_H) / 2;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        gfx.blit(COVER_TEXTURE, coverLeft, coverTop, COVER_DISPLAY_W, COVER_DISPLAY_H,
                COVER_SRC_U, COVER_SRC_V, COVER_SRC_W, COVER_SRC_H, COVER_TEX_W, COVER_TEX_H);

        layoutTabs(coverLeft, coverTop, COVER_DISPLAY_W, coverTop + COVER_DISPLAY_H);
        renderTabs(gfx);
    }

    @Override
    protected void onTabClicked(BookTab tab) {
        playPageTurnSound();
        this.minecraft.setScreen(new BookOpenScreen(tab));
    }
}