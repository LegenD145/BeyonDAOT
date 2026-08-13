package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ClientKnownIdentities;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * S2C — sent once on login (and on relog) to restore an observer's full set
 * of previously-revealed identities. Without this, persistentData survives
 * the restart server-side but the client's local ClientKnownIdentities cache
 * would start empty every relog, hiding nametags the player already earned.
 */
public record IdentityFullSyncPayload(List<java.util.UUID> knownUuids) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<IdentityFullSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "identity_full_sync"));

    public static final StreamCodec<FriendlyByteBuf, IdentityFullSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), IdentityFullSyncPayload::knownUuids,
                    IdentityFullSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(IdentityFullSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientKnownIdentities.setAll(payload.knownUuids()));
    }
}
