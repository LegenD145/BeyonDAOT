package com.aotaddon.client.book;

import com.aotaddon.AotAddon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * First screen shown when the book keybind is pressed: the closed cover
 * with all 5 sticky tabs visible and clickable right away.
 * Clicking any tab plays the page-turn sound and opens BookOpenScreen with
 * that tab pre-selected.
 */
public class BookCoverScreen extends BookScreenBase {

    private static final ResourceLocation COVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/gui/book.png");

    // book.png is a 230x162 canvas; bbox from PIL Image.getbbox() on the
    // current export: (65, 15, 163, 145).
    private static final int COVER_SRC_U = 65;
    private static final int COVER_SRC_V = 15;
    private static final int COVER_SRC_W = 98;
    private static final int COVER_SRC_H = 130;
    private static final int COVER_TEX_W = 230;
    private static final int COVER_TEX_H = 162;

    private static final int DISPLAY_SCALE = 2;
    private static final int COVER_DISPLAY_W = COVER_SRC_W * DISPLAY_SCALE;
    private static final int COVER_DISPLAY_H = COVER_SRC_H * DISPLAY_SCALE;

    private int coverLeft, coverTop;

    public BookCoverScreen() {
        super(Component.translatable("titanreqiuem.book.title"));
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
