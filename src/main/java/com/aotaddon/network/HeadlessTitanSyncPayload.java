package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.HeadlessTitanClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C visual marker that tells clients to render a titan body without its
 * head after the severed head entity has been spawned.
 */
public record HeadlessTitanSyncPayload(int entityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HeadlessTitanSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "headless_titan_sync"));

    public static final StreamCodec<FriendlyByteBuf, HeadlessTitanSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, HeadlessTitanSyncPayload::entityId,
                    HeadlessTitanSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HeadlessTitanSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HeadlessTitanClientState.markHeadless(payload.entityId()));
    }
}
