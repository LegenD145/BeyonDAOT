package com.aotaddon.client;
//the comments are crazy on this, i have illustrated everything for the ones if you wish to steal and fucking use my work lame ass dih
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Renders the Helos charge meter as stars below the crosshair.
 *
 * IMPORTANT: No import of LocalPlayer — it extends Player → Entity and
 * would trigger early Entity loading under Sinytra Connector.
 * Player is accessed via Minecraft.getInstance().player inline.
 */
@OnlyIn(Dist.CLIENT)
public class HelosHudRenderer {

    private static final int BELOW_CROSSHAIR_OFFSET = 16;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;
        if (!"helos".equals(ClientCardStats.getFamily())) return;
        if (mc.screen != null) return;

        int kills = ClientCardStats.getHelosKills();
        boolean ready = kills >= 5;

        String stars = buildStars(kills);
        String display = ready ? stars + " §6READY" : stars;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int centreX = screenW / 2;
        int centreY = screenH / 2;
        int textY = centreY + BELOW_CROSSHAIR_OFFSET;

        int textWidth = mc.font.width(net.minecraft.network.chat.Component.literal(display));
        int textX = centreX - (textWidth / 2);

        graphics.drawString(
                mc.font,
                display,
                textX,
                textY,
                ready ? 0xFFD700 : 0xFFFFFF,
                true
        );
    }

    private String buildStars(int kills) {
        String filled = "⭐";
        String empty  = "☆";

        int filledCount;
        if (kills >= 4)      filledCount = 3;
        else if (kills >= 2) filledCount = 2;
        else if (kills >= 1) filledCount = 1;
        else                 filledCount = 0;

        int emptyCount = 3 - filledCount;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filledCount; i++) sb.append(filled);
        for (int i = 0; i < emptyCount;  i++) sb.append(empty);
        return sb.toString();
    }
}