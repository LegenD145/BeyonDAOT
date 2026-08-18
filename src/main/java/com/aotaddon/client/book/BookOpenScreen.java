package com.aotaddon.client.book;

import com.aotaddon.AotAddon;
import com.aotaddon.client.book.page.BookRightPages;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

/**
 * Opened book: ONE spread texture (book_page.png) spanning both pages.
 * Left half swaps per active tab (stats, talents, ...). Right half is
 * always the character summary (name, splash, live avatar).
 *
 * Current book_page.png export is 230x162 landscape (already the open spread),
 * so it is blitted without the old -90° correction that applied to the
 * previous 162x230 portrait canvas.
 */
public class BookOpenScreen extends BookScreenBase {

    private static final ResourceLocation SPREAD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/gui/book_page.png");
    private static final ResourceLocation AVATAR_SPLASH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID,
                    "textures/gui/greybackground_for_character_avatar.png");

    private static final int SPREAD_NATIVE_W = 230;
    private static final int SPREAD_NATIVE_H = 162;

    // Match vanilla 6-row double-chest height (222). Width follows the
    // 230×162 native aspect so the spread isn't stretched (~315px).
    private static final int SPREAD_DISPLAY_H = 222;
    private static final int SPREAD_DISPLAY_W = SPREAD_DISPLAY_H * SPREAD_NATIVE_W / SPREAD_NATIVE_H;

    // greybackground bbox from PIL Image.getbbox(): (35, 127, 135, 208)
    private static final int SPLASH_SRC_U = 35;
    private static final int SPLASH_SRC_V = 127;
    private static final int SPLASH_SRC_W = 100;
    private static final int SPLASH_SRC_H = 81;
    private static final int SPLASH_TEX_W = 162;
    private static final int SPLASH_TEX_H = 230;

    static final int INK = 0x4A3F2A;

    private BookTab activeTab;
    private int spreadLeft, spreadTop;

    public BookOpenScreen(BookTab initialTab) {
        super(Component.translatable("titanreqiuem.book.title"));
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

        gfx.blit(SPREAD_TEXTURE, spreadLeft, spreadTop, SPREAD_DISPLAY_W, SPREAD_DISPLAY_H,
                0, 0, SPREAD_NATIVE_W, SPREAD_NATIVE_H, SPREAD_NATIVE_W, SPREAD_NATIVE_H);

        int leftRegionX = spreadLeft;
        int leftRegionW = SPREAD_DISPLAY_W / 2;
        int rightRegionX = spreadLeft + leftRegionW;

        BookRightPages.render(gfx, this.font, activeTab, leftRegionX, spreadTop, leftRegionW, SPREAD_DISPLAY_H);
        renderCharacterSummary(gfx, rightRegionX, spreadTop, leftRegionW, SPREAD_DISPLAY_H, mouseX, mouseY);

        // book_page.png opaque bbox (5,5,225,161). Tabs hug that leather rim,
        // not the full 230x162 blit which includes transparent padding.
        int visLeft = spreadLeft + SPREAD_DISPLAY_W * 5 / SPREAD_NATIVE_W;
        int visTop = spreadTop + SPREAD_DISPLAY_H * 5 / SPREAD_NATIVE_H;
        int visW = SPREAD_DISPLAY_W * 220 / SPREAD_NATIVE_W;
        int visH = SPREAD_DISPLAY_H * 156 / SPREAD_NATIVE_H;
        layoutTabs(visLeft, visTop, visW, visTop + visH);
        renderTabs(gfx);
    }

    private void renderCharacterSummary(GuiGraphics gfx, int regionX, int regionY, int regionW, int regionH,
                                        int mouseX, int mouseY) {
        Player player = this.minecraft.player;
        if (player == null) return;

        // TODO: Simple Nicknames reflection once that helper exists.
        Component nickname = player.getDisplayName();
        int titleX = regionX + (regionW / 2) - (this.font.width(nickname) / 2);
        int titleY = regionY + Math.max(28, regionH / 8);
        gfx.drawString(this.font, nickname, titleX, titleY, INK, false);

        int splashDisplayW = (int) (regionW * 0.72f);
        int splashDisplayH = (int) (regionH * 0.64f);
        int splashX = regionX + (regionW - splashDisplayW) / 2;
        int splashY = titleY + this.font.lineHeight + 6;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.35f);
        gfx.blit(AVATAR_SPLASH_TEXTURE, splashX, splashY, splashDisplayW, splashDisplayH,
                SPLASH_SRC_U, SPLASH_SRC_V, SPLASH_SRC_W, SPLASH_SRC_H, SPLASH_TEX_W, SPLASH_TEX_H);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        int avatarW = (int) (splashDisplayW * 0.70f);
        int avatarH = (int) (splashDisplayH * 0.88f);
        int avatarX = splashX + (splashDisplayW - avatarW) / 2;
        int avatarY = splashY + (splashDisplayH - avatarH) / 2;
        int avatarZoom = Math.max(1, 30 * avatarH / 71);

        gfx.enableScissor(splashX, splashY, splashX + splashDisplayW, splashY + splashDisplayH);
        renderStandingAvatar(gfx, avatarX, avatarY, avatarX + avatarW, avatarY + avatarH,
                avatarZoom, mouseX, mouseY, player);
        gfx.disableScissor();
    }

    /**
     * InventoryScreen draws the live player, so a campfire sit would show
     * seated. Temporarily stand (and detach the seat visually) for this blit only.
     */
    private void renderStandingAvatar(GuiGraphics gfx, int x1, int y1, int x2, int y2,
                                      int zoom, int mouseX, int mouseY, Player player) {
        Pose prevPose = player.getPose();
        Entity prevVehicle = player.getVehicle();
        setVehicleField(player, null);
        player.setPose(Pose.STANDING);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gfx, x1, y1, x2, y2, zoom, 0.0625f, mouseX, mouseY, player);
        } finally {
            setVehicleField(player, prevVehicle);
            player.setPose(prevPose);
        }
    }

    private static void setVehicleField(Entity passenger, Entity vehicle) {
        try {
            Field field = Entity.class.getDeclaredField("vehicle");
            field.setAccessible(true);
            field.set(passenger, vehicle);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Override
    protected void onTabClicked(BookTab tab) {
        if (tab == activeTab) return;
        playPageTurnSound();
        this.activeTab = tab;
    }
}
