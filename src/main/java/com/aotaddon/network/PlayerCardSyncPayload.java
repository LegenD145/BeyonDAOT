package com.aotaddon.network;

import com.aotaddon.AotAddon;
import com.aotaddon.access.ConsentManager;
import com.aotaddon.carry.CarryManager;
import com.aotaddon.client.ClientCardStats;
import com.aotaddon.currency.CurrencyFaction;
import com.aotaddon.family.FamilyData;
import com.aotaddon.reputation.ReputationData;
import com.aotaddon.rewards.CombatXpData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerCardSyncPayload(
        String family, String bloodline, double combatXp, int helosKills,
        int repParadis, int repMarley,
        boolean grabMode, boolean consentOpen
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerCardSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "player_card_sync"));

    public static final StreamCodec<FriendlyByteBuf, PlayerCardSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PlayerCardSyncPayload decode(FriendlyByteBuf buf) {
                    return new PlayerCardSyncPayload(
                            buf.readUtf(), buf.readUtf(), buf.readDouble(), buf.readVarInt(),
                            buf.readVarInt(), buf.readVarInt(),
                            buf.readBoolean(), buf.readBoolean()
                    );
                }

                @Override
                public void encode(FriendlyByteBuf buf, PlayerCardSyncPayload p) {
                    buf.writeUtf(p.family());
                    buf.writeUtf(p.bloodline());
                    buf.writeDouble(p.combatXp());
                    buf.writeVarInt(p.helosKills());
                    buf.writeVarInt(p.repParadis());
                    buf.writeVarInt(p.repMarley());
                    buf.writeBoolean(p.grabMode());
                    buf.writeBoolean(p.consentOpen());
                }
            };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player) {
        String family = FamilyData.getFamily(player);
        String bloodline = CurrencyFaction.readName(player);

        boolean grab = CarryManager.isEnabled(player);
        boolean consent = ConsentManager.get(player.getServer()).isOpen(player);

        PacketDistributor.sendToPlayer(player, new PlayerCardSyncPayload(
                family == null ? "" : family,
                bloodline == null ? "" : bloodline,
                CombatXpData.getBalance(player),
                FamilyData.getHelosKills(player),
                ReputationData.getParadis(player),
                ReputationData.getMarley(player),
                grab, consent
        ));
    }

    public static void handle(PlayerCardSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCardStats.set(
                payload.family(), payload.bloodline(), payload.combatXp(), payload.helosKills(),
                payload.repParadis(), payload.repMarley(),
                payload.grabMode(), payload.consentOpen()
        ));
    }
}
