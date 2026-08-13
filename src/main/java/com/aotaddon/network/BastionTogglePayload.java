package com.aotaddon.network;
// tatake or sum
import com.aotaddon.AotAddon;
import com.aotaddon.util.BastionStateHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
// TATKAKTEE
public record BastionTogglePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BastionTogglePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "bastion_toggle"));

    public static final StreamCodec<FriendlyByteBuf, BastionTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new BastionTogglePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BastionTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                boolean active = BastionStateHandler.toggle(player);
                player.sendSystemMessage(Component.literal(
                        active ? "§a[Bastion] Activated." : "§c[Bastion] Deactivated."
                ));
            }
        });
    }
}
