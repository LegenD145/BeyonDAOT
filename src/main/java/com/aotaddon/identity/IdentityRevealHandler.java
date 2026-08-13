package com.aotaddon.identity;

import com.aotaddon.network.IdentityFullSyncPayload;
import com.aotaddon.network.RevealIdentityPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Anti-metagaming identity reveal — NOT annotated with @EventBusSubscriber,
 * registered manually via NeoForge.EVENT_BUS.addListener() in AotAddon,
 * same convention as FamilyEventHandler.
 *
 * Trigger: a player says a phrase matching TRIGGER_PATTERN anywhere in their
 * message (e.g. "Hello, my name is Kronoks."). Every OTHER player within
 * REVEAL_RADIUS blocks of the speaker at that instant learns the speaker's
 * identity permanently (persisted per-observer). Players outside the radius
 * learn nothing until the speaker triggers the phrase again while closer.
 */
public class IdentityRevealHandler {

    /** Matches "my name is" anywhere in the message, case-insensitive. */
    private static final Pattern TRIGGER_PATTERN =
            Pattern.compile("\\bmy\\s+name\\s+is\\b", Pattern.CASE_INSENSITIVE);

    public static final double REVEAL_RADIUS = 50.0;

    public static void onChatMessage(ServerChatEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer speaker)) return;

        String message = event.getMessage().getString();
        if (!TRIGGER_PATTERN.matcher(message).find()) return;

        ServerLevel level = speaker.serverLevel();
        AABB box = speaker.getBoundingBox().inflate(REVEAL_RADIUS);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(
                ServerPlayer.class, box,
                p -> !p.getUUID().equals(speaker.getUUID())
        );

        for (ServerPlayer observer : nearby) {
            boolean newlyRevealed = IdentityRevealData.reveal(observer, speaker.getUUID());
            if (newlyRevealed) {
                PacketDistributor.sendToPlayer(observer, new RevealIdentityPayload(speaker.getUUID()));
            }
        }
    }

    /** Restores the client-side cache on login/relog, since it starts empty. */
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        List<java.util.UUID> known = IdentityRevealData.getKnownIdentities(player).stream().toList();
        PacketDistributor.sendToPlayer(player, new IdentityFullSyncPayload(known));
    }
}
