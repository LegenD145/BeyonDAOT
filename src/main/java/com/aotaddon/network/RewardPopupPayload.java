package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.RewardPopupManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * S2C — carries the reward text lines for one titan kill so the client can
 * show them near the crosshair. Registered in AotAddon.registerPayloads()
 * via registrar.playToClient(...), same as ShiftlockStateSyncPayload.
 */
public record RewardPopupPayload(List<String> lines) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RewardPopupPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "reward_popup"));

    public static final StreamCodec<FriendlyByteBuf, RewardPopupPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), RewardPopupPayload::lines,
                    RewardPopupPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RewardPopupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RewardPopupManager.push(payload.lines()));
    }
}