package com.aotaddon.client.book;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Opened book: ONE spread texture (book_page.png) spanning both pages -
 * it is not two separate page images. Left half is always the character
 * summary (nickname title, grey splash backdrop, live avatar render) and
 * never changes. Right half swaps per active tab - empty for this first
 * pass, just whatever part of the spread texture shows through there.
 *
 * book_page.png is authored rotated 90° (portrait canvas holding what's
 * meant to be a landscape two-page spread), same quirk as Book.png, so we
 * counter-rotate it at render time rather than asking for a re-export.
 * NOTE: rotation direction (-90 below) is a guess since I can't preview
 * the render myself - flip to +90 if it comes out mirrored/upside down.
 */
public class BookOpenScreen extends BookScreenBase {

    private static final ResourceLocation SPREAD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("titanreqiuem", "textures/gui/book_page.png");
    private static final ResourceLocation AVATAR_SPLASH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("titanreqiuem",
                    "textures/gui/greybackground_for_character_avatar.png");

    // book_page.png native canvas, BEFORE the 90° correction is applied.
    private static final int SPREAD_NATIVE_W = 162;
    private static final int SPREAD_NATIVE_H = 230;

    // Final on-screen spread size (post-rotation, landscape). Height derives
    // from native aspect so the art isn't stretched.
    private static final int SPREAD_DISPLAY_W = 648;
    private static final int SPREAD_DISPLAY_H = SPREAD_DISPLAY_W * SPREAD_NATIVE_W / SPREAD_NATIVE_H;

    // Grey splash art only occupies a small region of its own 162x230 canvas
    // (bbox measured directly from the file). Crop to that region so it
    // actually reads as a backdrop instead of a faint smudge.
    private static final int SPLASH_SRC_U = 35;
    private static final int SPLASH_SRC_V = 127;
    private static final int SPLASH_SRC_W = 100;
    private static final int SPLASH_SRC_H = 81;
    private static final int SPLASH_TEX_W = 162;
    private static final int SPLASH_TEX_H = 230;

    private BookTab activeTab;
    private int spreadLeft, spreadTop;

    public BookOpenScreen(BookTab initialTab) {
        super(Component.literal("Book"));
        this.activeTab = initialTab;
    }

    @Override
    protected void init() {
        super.init();
        this.spreadLeft = (this.width - SPREAD_DISPLAY_W) / 2;
        this.spreadTop = (this.height - SPREAD_DISPLAY_H) / 2;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        // --- Single spread texture, rotated into place, covering both pages ---
        blitRotated90(gfx, SPREAD_TEXTURE, spreadLeft, spreadTop, SPREAD_DISPLAY_W, SPREAD_DISPLAY_H,
                SPREAD_NATIVE_W, SPREAD_NATIVE_H);

        int leftRegionX = spreadLeft;
        int leftRegionW = SPREAD_DISPLAY_W / 2;
        // int rightRegionX = spreadLeft + leftRegionW; // reintroduce once right-page content exists

        // --- Left half: fixed character summary, never changes per-tab ---
        renderCharacterSummary(gfx, leftRegionX, spreadTop, leftRegionW, SPREAD_DISPLAY_H, mouseX, mouseY);

        // --- Right half: swaps per active tab. Empty for now. ---
        // TODO: per-tab right-page content goes here once each tab is built out.
        // activeTab currently unused beyond this switch point.

        layoutTabs(spreadLeft, spreadTop, SPREAD_DISPLAY_W, spreadTop + SPREAD_DISPLAY_H);
        renderTabs(gfx);
    }

    private void renderCharacterSummary(GuiGraphics gfx, int regionX, int regionY, int regionW, int regionH,
                                        int mouseX, int mouseY) {
        Player player = this.minecraft.player;
        if (player == null) return;

        // Nickname title, top of the left page.
        // TODO: swap to Simple Nicknames mod lookup once its reflection helper
        // is available - falls back to vanilla display name for now.
        Component nickname = player.getDisplayName();
        int titleX = regionX + (regionW / 2) - (this.font.width(nickname) / 2);
        int titleY = regionY + 22;
        gfx.drawString(this.font, nickname, titleX, titleY, 0x4A3F2A, false);

        // Grey splash backdrop, cropped to its real content. Sized/positioned
        // per Alex's mockup: moderate size (not blown up to fill the page),
        // sitting lower-left rather than spanning top-to-bottom.
        int splashDisplayW = (int) (regionW * 0.45f);
        int splashDisplayH = splashDisplayW * SPLASH_SRC_H / SPLASH_SRC_W;
        int splashX = regionX + (int) (regionW * 0.08f);
        int splashY = regionY + (int) (regionH * 0.40f);

        gfx.blit(AVATAR_SPLASH_TEXTURE, splashX, splashY, splashDisplayW, splashDisplayH,
                SPLASH_SRC_U, SPLASH_SRC_V, SPLASH_SRC_W, SPLASH_SRC_H, SPLASH_TEX_W, SPLASH_TEX_H);

        // Avatar rendered fitted to the splash area, same underlying
        // entity-render approach vanilla uses for the InventoryScreen player
        // preview - eyes follow the real cursor, matching Alex's ask.
        //
        // IMPORTANT: the "size" param below is NOT half the box dimensions -
        // it's an independent zoom/scale value (vanilla's own player preview
        // uses a small fixed number like 30 regardless of box size). Passing
        // a value derived from the box caused a massively oversized model +
        // shadow blob spilling outside the page. Scissor-clip as a safety
        // net too, since this method doesn't clip overflow on its own.
        int avatarCenterX = splashX + splashDisplayW / 2;
        int avatarTop = splashY;
        int avatarBottom = splashY + splashDisplayH;
        int avatarBoxHalfWidth = Math.min(splashDisplayW, splashDisplayH) / 2;
        int avatarZoomSize = 30;

        gfx.enableScissor(splashX, splashY, splashX + splashDisplayW, splashY + splashDisplayH);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                gfx,
                avatarCenterX - avatarBoxHalfWidth, avatarTop,
                avatarCenterX + avatarBoxHalfWidth, avatarBottom,
                avatarZoomSize,
                0.0625f,
                mouseX, mouseY,
                player
        );
        gfx.disableScissor();
    }

    /**
     * Draws a texture rotated 90° around its own center, scaled to fill the
     * given destination box. Native W/H are the texture's dimensions BEFORE
     * rotation; after rotation the box's width corresponds to native H and
     * the box's height corresponds to native W.
     */
    private void blitRotated90(GuiGraphics gfx, ResourceLocation texture, int destLeft, int destTop,
                               int destW, int destH, int nativeW, int nativeH) {
        float scale = destW / (float) nativeH;

        gfx.pose().pushPose();
        gfx.pose().translate(destLeft + destW / 2.0, destTop + destH / 2.0, 0);
        gfx.pose().mulPose(Axis.ZP.rotationDegrees(-90));
        gfx.pose().scale(scale, scale, 1f);
        gfx.pose().translate(-nativeW / 2.0, -nativeH / 2.0, 0);
        gfx.blit(texture, 0, 0, nativeW, nativeH, 0, 0, nativeW, nativeH, nativeW, nativeH);
        gfx.pose().popPose();
    }

    @Override
    protected void onTabClicked(BookTab tab) {
        if (tab == activeTab) return;
        playPageTurnSound();
        this.activeTab = tab;
    }
}