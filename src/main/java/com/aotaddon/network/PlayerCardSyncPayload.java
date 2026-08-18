package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ClientCardStats;
import com.aotaddon.currency.CurrencyFaction;
import com.aotaddon.family.FamilyData;
import com.aotaddon.rewards.CombatXpData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerCardSyncPayload(String family, String bloodline, double combatXp)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerCardSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "player_card_sync"));

    public static final StreamCodec<FriendlyByteBuf, PlayerCardSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PlayerCardSyncPayload::family,
                    ByteBufCodecs.STRING_UTF8, PlayerCardSyncPayload::bloodline,
                    ByteBufCodecs.DOUBLE, PlayerCardSyncPayload::combatXp,
                    PlayerCardSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player) {
        String family = FamilyData.getFamily(player);
        String bloodline = CurrencyFaction.readName(player);
        PacketDistributor.sendToPlayer(player, new PlayerCardSyncPayload(
                family == null ? "" : family,
                bloodline == null ? "" : bloodline,
                CombatXpData.getBalance(player)
        ));
    }

    public static void handle(PlayerCardSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCardStats.set(payload.family(), payload.bloodline(), payload.combatXp()));
    }
}
