package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.pd.ClientPdState;
import com.aotaddon.pd.PdType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders the PD skull icon at the very top-middle of the screen whenever the
 * local player is currently under any active PD type (PD / Soft PD / Extinction).
 * Hidden entirely when PD is NONE.
 *
 * Register via: NeoForge.EVENT_BUS.register(new PdSkullOverlay());
 * inside the Dist.CLIENT block in AotAddon's constructor.
 *
 * NOTE: ClientPdState is currently a placeholder that nothing writes to yet —
 * this will render nothing until the real PdState/command system on the
 * server side syncs a non-NONE value into it (same sync pattern as
 * HonorSyncPayload -> ClientHonorData).
 */
@OnlyIn(Dist.CLIENT)
public class PdSkullOverlay {

    private static final ResourceLocation SKULL_ICON =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/gui/pd_active.png");

    private static final int ICON_SIZE = 32;      // rendered 1:1 with source texture
    private static final int TEXTURE_SIZE = 64;   // actual pd_active.png dimensions
    private static final int TOP_MARGIN = 4;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;
        if (ClientPdState.get() == PdType.NONE) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = (screenW - ICON_SIZE) / 2;
        int y = TOP_MARGIN;

        graphics.blit(SKULL_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}