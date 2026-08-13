package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.gear.GearPouchHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenGearPouchPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenGearPouchPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "open_gear_pouch")
            );

    public static final StreamCodec<FriendlyByteBuf, OpenGearPouchPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenGearPouchPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenGearPouchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GearPouchHelper.openForPlayer(player);
            }
        });
    }
}