package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.util.DodgeIFrameHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client->server the moment a Dodge (double-tap S) fires.
 * Grants the player a brief window of damage immunity, tracked server-side
 * in DodgeIFrameHandler so LivingIncomingDamageEvent can cancel damage.
 */
public record DodgeStartPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DodgeStartPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "dodge_start"));

    public static final StreamCodec<FriendlyByteBuf, DodgeStartPayload> STREAM_CODEC =
            StreamCodec.unit(new DodgeStartPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DodgeStartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DodgeIFrameHandler.grantIFrames(player);
            }
        });
    }
}
