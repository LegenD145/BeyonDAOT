package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ClientCurrencyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C — the player's current currency balance (Medals for Eldian, Banknotes
 * for Marley — server already resolved which one before sending; the client
 * just displays the number). No digit cap: plain int, formatted with
 * String.valueOf, no truncation/rounding.
 */
public record CurrencySyncPayload(int balance) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CurrencySyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "currency_sync"));

    public static final StreamCodec<FriendlyByteBuf, CurrencySyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CurrencySyncPayload::balance,
                    CurrencySyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CurrencySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCurrencyState.setBalance(payload.balance()));
    }
}