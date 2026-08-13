package com.aotaddon.access;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleConsentC2SPacket() implements CustomPacketPayload {

    public static final Type<ToggleConsentC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("titanreqiuem", "toggle_consent"));

    public static final StreamCodec<ByteBuf, ToggleConsentC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleConsentC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleConsentC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ConsentManager manager = ConsentManager.get(player.getServer());
            boolean newState = manager.toggle(player);

            player.displayClientMessage(
                    Component.literal("Consent Mode: " + (newState ? "ON" : "OFF"))
                            .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true
            );
        });
    }
}