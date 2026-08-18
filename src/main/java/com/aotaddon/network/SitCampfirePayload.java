package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.campfire.CampfireSitHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SitCampfirePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SitCampfirePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "sit_campfire"));

    public static final StreamCodec<FriendlyByteBuf, SitCampfirePayload> STREAM_CODEC =
            StreamCodec.unit(new SitCampfirePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SitCampfirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CampfireSitHandler.trySit(player);
            }
        });
    }
}
