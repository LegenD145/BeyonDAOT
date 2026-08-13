package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.util.OdmXpHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client->server whenever gas is consumed on the client side via
 * DannysAot.consumeGasFromGear (which runs in ODMTickHandler on the client).
 * The server handler calls OdmXpHandler.grantGasXp() so XP is written
 * into the correct KubeJSPersistentData compound where st_getXP() reads it.
 */
public record OdmGasXpPayload(int gasSpent) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OdmGasXpPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "odm_gas_xp"));

    public static final StreamCodec<FriendlyByteBuf, OdmGasXpPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OdmGasXpPayload::gasSpent,
                    OdmGasXpPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OdmGasXpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.gasSpent() <= 0) return;
            AotAddon.LOGGER.debug("[OdmGasXpPayload] Received {} gas spent for {}", payload.gasSpent(), player.getName().getString());
            OdmXpHandler.grantGasXp(player, payload.gasSpent());
        });
    }
}
