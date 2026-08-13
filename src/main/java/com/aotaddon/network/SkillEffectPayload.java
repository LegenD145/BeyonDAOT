package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.util.OdmXpHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client->server when Impulse or Dodge fires, so the burst particle
 * effect is visible to every nearby player, not just the one who dashed.
 *
 * skillId: 0 = Impulse, 1 = Dodge — different particle color/style for each.
 */
public record SkillEffectPayload(int skillId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillEffectPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "skill_effect"));

    public static final StreamCodec<FriendlyByteBuf, SkillEffectPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SkillEffectPayload::skillId,
                    SkillEffectPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            // This payload is sent only after Impulse/Dodge successfully fires.
            // skillId 0 = Impulse, skillId 1 = Dodge.
            if (payload.skillId() == 0 || payload.skillId() == 1) {
                OdmXpHandler.grantXp(player, 0.3f);
            }

            if (payload.skillId() == 0) {
                com.aotaddon.util.ImpulseWindowHandler.markImpulseFired(player.getUUID());
            }

            double x = player.getX();
            double y = player.getY() + player.getBbHeight() * 0.5;
            double z = player.getZ();

            if (payload.skillId() == 0) {
                // Impulse — sharp white/cloud burst
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 18, 0.4, 0.4, 0.4, 0.08);
                level.sendParticles(ParticleTypes.POOF, x, y, z, 6, 0.2, 0.2, 0.2, 0.02);
            } else {
                // Dodge — sharper, more compact burst (defensive feel)
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.25, 0.25, 0.25, 0.05);
                level.sendParticles(ParticleTypes.CRIT, x, y, z, 12, 0.3, 0.3, 0.3, 0.1);
            }
        });
    }
}