package com.aotaddon.client;

import com.aotaddon.pd.ClientLifeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Replaces vanilla's health bar with a life-count-tinted version using
 * VANILLA'S OWN heart sprites (hardcore / frozen_hardcore / withered_hardcore),
 * chosen by the player's current life count:
 *   3 lives -> hardcore (red)
 *   2 lives -> frozen_hardcore (ice blue)
 *   1 life  -> withered_hardcore (near-black)
 *
 * Damage flash is vanilla's own "_blinking" sprite variant (shown while
 * hurtTime > 0) — no custom flash logic needed. Half-hearts use the
 * matching *_half sprite. Empty/background hearts use container_hardcore.
 *
 * Register via: NeoForge.EVENT_BUS.register(new CustomHeartOverlay());
 * inside the Dist.CLIENT block in AotAddon's constructor.
 *
 * NOTE: reads ClientLifeState.getLives() — a placeholder client cache,
 * same pattern as ClientHonorData/ClientPdState. Wire a real sync packet
 * to it once LifeData/the PD life system exists server-side; defaults to
 * 3 (red) so nothing looks broken before that's wired up.
 */
@OnlyIn(Dist.CLIENT)
public class CustomHeartOverlay {

    private static final int HEART_SIZE = 9;
    private static final int HEART_SPACING = 8;
    private static final int ROW_MARGIN_LEFT = 1;

    @SubscribeEvent
    public void onRenderHealth(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        event.setCanceled(true); // fully replace vanilla's health layer

        String prefix = switch (ClientLifeState.getLives()) {
            case 3 -> "hardcore";
            case 2 -> "frozen_hardcore";
            default -> "withered_hardcore"; // 1 life (and as a safe fallback for 0)
        };

        boolean flash = player.hurtTime > 0;
        String suffix = flash ? "_blinking" : "";

        ResourceLocation containerSprite = vanilla("hud/heart/container_hardcore" + suffix);
        ResourceLocation fullSprite = vanilla("hud/heart/" + prefix + "_full" + suffix);
        ResourceLocation halfSprite = vanilla("hud/heart/" + prefix + "_half" + suffix);

        int maxHealth = Math.round(player.getMaxHealth());
        int health = Math.round(player.getHealth());
        int maxHearts = (maxHealth + 1) / 2;
        int fullHearts = health / 2;
        boolean hasHalfHeart = (health % 2) == 1;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int rowY = screenH - 39; // vanilla's default health row sits ~39px above the bottom
        int rowX = screenW / 2 - 91 + ROW_MARGIN_LEFT;

        for (int i = 0; i < maxHearts; i++) {
            int x = rowX + i * HEART_SPACING;
            graphics.blitSprite(containerSprite, x, rowY, HEART_SIZE, HEART_SIZE);

            if (i < fullHearts) {
                graphics.blitSprite(fullSprite, x, rowY, HEART_SIZE, HEART_SIZE);
            } else if (i == fullHearts && hasHalfHeart) {
                graphics.blitSprite(halfSprite, x, rowY, HEART_SIZE, HEART_SIZE);
            }
        }
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}