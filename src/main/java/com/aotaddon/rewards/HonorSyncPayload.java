package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ClientHonorData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C — pushes the player's current Honor Point balance to their client so
 * the bottom-left HUD actually updates. persistentData is server-only and
 * never auto-syncs, so this packet is required any time the balance changes
 * (and once on login so the HUD isn't stuck at 0 on join).
 */
public record HonorSyncPayload(double balance) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HonorSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "honor_sync"));

    public static final StreamCodec<FriendlyByteBuf, HonorSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, HonorSyncPayload::balance,
                    HonorSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HonorSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHonorData.setBalance(payload.balance()));
    }
}