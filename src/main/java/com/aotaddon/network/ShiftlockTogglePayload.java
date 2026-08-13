package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.util.ShiftlockStateHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShiftlockTogglePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShiftlockTogglePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "shiftlock_toggle"));

    public static final StreamCodec<FriendlyByteBuf, ShiftlockTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new ShiftlockTogglePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShiftlockTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShiftlockStateHandler.ToggleResult result = ShiftlockStateHandler.toggle(player);
                boolean nowActive = result == ShiftlockStateHandler.ToggleResult.ENABLED;

                switch (result) {
                    case ENABLED -> player.sendSystemMessage(Component.literal("§a[Shiftlock] Enabled."));
                    case DISABLED -> player.sendSystemMessage(Component.literal("§c[Shiftlock] Disabled."));
                    case NOT_IN_TITAN_FORM -> player.sendSystemMessage(Component.literal(
                            "§c[Shiftlock] You must be in titan form to use this."));
                }

                // Tell the client the definitive state so its local yaw-override tick
                // (which is what actually fixes the visual lock for the driving player)
                // matches server truth, rather than optimistically flipping on keypress.
                PacketDistributor.sendToPlayer(player, new ShiftlockStateSyncPayload(nowActive));
            }
        });
    }
}