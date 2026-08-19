package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.pd.ClientLifeState;
import com.aotaddon.pd.ClientPdState;
import com.aotaddon.pd.PdLifeData;
import com.aotaddon.pd.PdType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PdLifeSyncPayload(int pdTypeOrdinal, int lives) implements CustomPacketPayload {
    public static final Type<PdLifeSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "pd_life_sync"));

    public static final StreamCodec<FriendlyByteBuf, PdLifeSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.pdTypeOrdinal());
                        buf.writeVarInt(payload.lives());
                    },
                    buf -> new PdLifeSyncPayload(buf.readVarInt(), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player) {
        PdType pd = PdLifeData.getPdType(player);
        int lives = PdLifeData.getLives(player);
        PacketDistributor.sendToPlayer(player, new PdLifeSyncPayload(pd.ordinal(), lives));
    }

    public static void handle(PdLifeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            PdType[] values = PdType.values();
            PdType pd = (payload.pdTypeOrdinal() >= 0 && payload.pdTypeOrdinal() < values.length)
                    ? values[payload.pdTypeOrdinal()]
                    : PdType.NONE;
            ClientPdState.set(pd);
            ClientLifeState.setLives(Math.max(0, Math.min(3, payload.lives())));
        });
    }
}
