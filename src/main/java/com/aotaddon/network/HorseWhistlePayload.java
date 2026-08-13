package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.horse.HorseWhistleHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HorseWhistlePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HorseWhistlePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "horse_whistle"));

    public static final StreamCodec<FriendlyByteBuf, HorseWhistlePayload> STREAM_CODEC =
            StreamCodec.unit(new HorseWhistlePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HorseWhistlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HorseWhistleHandler.onSummon(player);
            }
        });
    }
}