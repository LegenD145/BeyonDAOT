package com.aotaddon.carry;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleGrabModeC2SPacket() implements CustomPacketPayload {

    public static final Type<ToggleGrabModeC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("titanreqiuem", "toggle_grab_mode"));

    public static final StreamCodec<ByteBuf, ToggleGrabModeC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleGrabModeC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleGrabModeC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean enabled = CarryManager.toggle(player);
            player.displayClientMessage(
                    Component.literal("Grab Mode: " + (enabled ? "ON" : "OFF"))
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true
            );
        });
    }
}