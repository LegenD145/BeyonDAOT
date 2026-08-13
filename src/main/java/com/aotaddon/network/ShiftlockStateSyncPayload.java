package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ShiftlockClientState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C confirmation of the actual server-side shiftlock state after a toggle attempt.
 * The client waits for this instead of optimistically flipping locally, so a rejected
 * toggle (e.g. player wasn't actually in titan form) can't leave client and server
 * disagreeing about whether the local yaw-override should be running.
 */
public record ShiftlockStateSyncPayload(boolean active) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShiftlockStateSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "shiftlock_state_sync"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ShiftlockStateSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ShiftlockStateSyncPayload::active,
                    ShiftlockStateSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShiftlockStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ShiftlockClientState.setActive(payload.active()));
    }
}