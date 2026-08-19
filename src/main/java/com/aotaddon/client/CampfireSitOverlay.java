package com.aotaddon.client;

import com.aotaddon.campfire.CampfireHelper;
import com.aotaddon.client.book.BookKeybind;
import com.aotaddon.network.SitCampfirePayload;
import com.aotaddon.client.ClientCombatTagState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Draws "[E] Sit" above the hotbar when a lit campfire is in range, and
 * steals the inventory key (E) so it sits the player and opens the book cover
 * instead of opening inventory. First E sits; second E (while seated) opens the book.
 */
@OnlyIn(Dist.CLIENT)
public class CampfireSitOverlay {

    private static final int PROMPT_Y_FROM_BOTTOM = 50;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int INK = 0xFFFFFF;

    private long lastScanTick = Long.MIN_VALUE;
    private boolean cachedNearFire = false;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!canShowPrompt()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Component text = Component.translatable(
                isOnCampfireSeat() ? "titanreqiuem.campfire.reopen" : "titanreqiuem.campfire.sit",
                mc.options.keyInventory.getTranslatedKeyMessage());
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = (screenW - mc.font.width(text)) / 2;
        int y = screenH - PROMPT_Y_FROM_BOTTOM;
        graphics.drawString(mc.font, text, x, y, INK, true);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!canShowPrompt()) {
            return;
        }
        while (mc.options.keyInventory.consumeClick()) {
            triggerSitOrReopen();
            break;
        }
    }

    @SubscribeEvent
    public void onScreenOpening(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) {
            return;
        }
        if (!isEligibleToSit()) {
            return;
        }
        event.setCanceled(true);
        triggerSitOrReopen();
    }

    private void triggerSitOrReopen() {
        if (isOnCampfireSeat()) {
            BookKeybind.openCover();
            return;
        }
        PacketDistributor.sendToServer(new SitCampfirePayload());
    }

    private boolean canShowPrompt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return false;
        }
        return isEligibleToSit();
    }

    private boolean isEligibleToSit() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return false;
        }
        if (isOnCampfireSeat(player)) {
            return true;
        }
        if (!player.onGround() && !isOnCampfireSeat(player)) {
            return false;
        }
        if (player.isPassenger() && !isOnCampfireSeat(player)) {
            return false;
        }
        if (ClientCombatTagState.isInCombat()) {
            return false;
        }
        long tick = mc.level.getGameTime();
        if (tick != lastScanTick && (lastScanTick == Long.MIN_VALUE || tick % SCAN_INTERVAL_TICKS == 0)) {
            lastScanTick = tick;
            cachedNearFire = CampfireHelper.isNearLitCampfire(mc.level, player.blockPosition());
        }
        return cachedNearFire;
    }

    private boolean isOnCampfireSeat() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && isOnCampfireSeat(mc.player);
    }

    private static boolean isOnCampfireSeat(LocalPlayer player) {
        return player.getVehicle() instanceof com.aotaddon.campfire.CampfireSeatEntity;
    }
}
