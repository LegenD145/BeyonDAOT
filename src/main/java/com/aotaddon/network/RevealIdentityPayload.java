package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ClientKnownIdentities;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C — sent only to the observer(s) who just learned a speaker's identity
 * via the "my name is X" trigger phrase (IdentityRevealHandler). NOT a
 * broadcast — recipients are chosen server-side by the 50-block radius scan.
 */
public record RevealIdentityPayload(java.util.UUID revealedUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RevealIdentityPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "reveal_identity"));

    public static final StreamCodec<FriendlyByteBuf, RevealIdentityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, RevealIdentityPayload::revealedUuid,
                    RevealIdentityPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RevealIdentityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientKnownIdentities.reveal(payload.revealedUuid()));
    }
}
