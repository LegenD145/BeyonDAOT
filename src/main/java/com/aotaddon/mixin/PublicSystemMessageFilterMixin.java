package com.aotaddon.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anti-metagaming: filters vanilla join/leave/death system messages out of
 * the chat log entirely. Does NOT touch player chat (suppressed server-side
 * via ChatBroadcastSuppressor cancelling ServerChatEvent) and does NOT touch
 * private feedback like /balance output or the starting-bonus message,
 * since those are built with Component.literal(...) rather than a matching
 * translation key and pass straight through unfiltered.
 *
 * Matches on translation key rather than message text so this survives
 * resource-pack/language changes:
 *   join  -> "multiplayer.player.joined" (+ ".renamed" variant)
 *   leave -> "multiplayer.player.left"
 *   death -> anything under "death.attack." (covers every vanilla cause)
 *
 * NOTE: targets the public single-argument addMessage(Component) overload,
 * which is what ClientPacketListener's system-chat handling calls into on
 * this NeoForge/MC version as of this mod's last verification. If the mixin
 * fails to apply after a game/mapping update, check this method's exact
 * signature against the decompiled vanilla class first.
 */
@Mixin(ChatComponent.class)
public class PublicSystemMessageFilterMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void aotaddon$filterPublicBroadcasts(Component message, CallbackInfo ci) {
        if (isSuppressed(message)) {
            ci.cancel();
        }
    }

    private static boolean isSuppressed(Component message) {
        if (!(message.getContents() instanceof TranslatableContents translatable)) return false;
        String key = translatable.getKey();
        return key.equals("multiplayer.player.joined")
                || key.equals("multiplayer.player.joined.renamed")
                || key.equals("multiplayer.player.left")
                || key.startsWith("death.attack.");
    }
}
