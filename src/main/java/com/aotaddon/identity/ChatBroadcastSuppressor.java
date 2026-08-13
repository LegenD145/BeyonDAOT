package com.aotaddon.identity;

import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Anti-metagaming: suppresses ALL player chat from vanilla's default
 * broadcast/log pipeline. Chat Bubbles has its own independent packet
 * (registered with receiveCanceled = true in their listener), so cancelling
 * here does not affect bubble visibility — only the vanilla ChatComponent
 * log that would otherwise let onlookers correlate WHO said WHAT after the
 * fact by scrolling back.
 *
 * Deliberately unconditional — every player chat message is suppressed, not
 * just ones matching the identity-reveal trigger phrase. Kept separate from
 * IdentityRevealHandler (single responsibility); both listen on
 * ServerChatEvent independently and neither depends on the other's
 * cancellation state, so registration order between them doesn't matter.
 *
 * NOT annotated with @EventBusSubscriber — registered manually in AotAddon.
 */
public class ChatBroadcastSuppressor {

    public static void onChatMessage(ServerChatEvent event) {
        event.setCanceled(true);
    }
}
