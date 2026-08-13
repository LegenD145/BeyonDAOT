package com.aotaddon.network;

import com.aotaddon.AotAddon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client->server each tick while the post-dash trail is active.
 * Spawns a single small particle puff at the player's current position
 * so other players see a streak as velocity decays.
 *
 * Kept as a separate, tiny payload from SkillEffectPayload since this one
 * fires multiple times per dash (once per tick for ~5-8 ticks) while the
 * burst only fires once.
 */
public record TrailEffectPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TrailEffectPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "trail_effect"));

    public static final StreamCodec<FriendlyByteBuf, TrailEffectPayload> STREAM_CODEC =
            StreamCodec.unit(new TrailEffectPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TrailEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            double x = player.getX();
            double y = player.getY() + player.getBbHeight() * 0.5;
            double z = player.getZ();

            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);
        });
    }
}
