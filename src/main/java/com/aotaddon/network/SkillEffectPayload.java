package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.util.ImpulseWindowHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S — client notifies server that a skill effect fired (e.g. Impulse dash).
 * Used to open server-side windows for decapitation checks, etc.
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
            if (payload.skillId() == 0) {
                ImpulseWindowHandler.markImpulseFired(player.getUUID());
            }
        });
    }
}
