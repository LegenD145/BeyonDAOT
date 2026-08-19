package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ClientCombatTagState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C — seconds remaining on the player's combat tag (0 = not tagged).
 */
public record CombatTagSyncPayload(int secondsLeft) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CombatTagSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "combat_tag_sync"));

    public static final StreamCodec<FriendlyByteBuf, CombatTagSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CombatTagSyncPayload::secondsLeft,
                    CombatTagSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombatTagSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCombatTagState.setSecondsLeft(payload.secondsLeft()));
    }
}
