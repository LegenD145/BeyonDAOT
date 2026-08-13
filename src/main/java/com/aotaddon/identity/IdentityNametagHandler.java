package com.aotaddon.identity;

import com.aotaddon.client.ClientKnownIdentities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Anti-metagaming nametag gate — client-side only. Cancels the vanilla
 * nametag render entirely (no placeholder, per design) for any Player entity
 * whose identity the LOCAL client hasn't been revealed to yet.
 *
 * Registered on NeoForge.EVENT_BUS inside the Dist.CLIENT block in AotAddon,
 * same convention as the other client-only overlay handlers.
 *
 * Chat Bubbles' own nametag-suppression (its "hideNametag" option) reads
 * this same event, so no conflict — both listeners just decide independently
 * whether the tag is allowed to render this frame.
 */
public class IdentityNametagHandler {

    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        // Always see your own nametag (not that it renders in first person anyway).
        if (target == Minecraft.getInstance().player) return;

        if (!ClientKnownIdentities.knows(target.getUUID())) {
            event.setCanRender(TriState.FALSE);
        }
    }
}
