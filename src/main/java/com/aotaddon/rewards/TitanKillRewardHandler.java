package com.aotaddon.rewards;

import com.aotaddon.currency.BanknoteData;
import com.aotaddon.currency.CurrencyFaction;
import com.aotaddon.currency.MedalData;
import com.aotaddon.network.PdLifeSyncPayload;
import com.aotaddon.network.PlayerCardSyncPayload;
import com.aotaddon.network.RewardPopupPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Register in AotAddon's constructor:
 *   NeoForge.EVENT_BUS.addListener(TitanKillRewardHandler::onLivingDeath);
 *
 * Current scope:
 *   - Honor Points: awarded to any killer, via HonorData.
 *   - Medals/Banknotes: routed by DAOT bloodline — Eldian killers get medals,
 *     Marley killers get banknotes, other bloodlines get neither.
 *   - Reputation: writes directly into Java-side ReputationData and appears in
 *     book/overlay sync via PlayerCardSyncPayload.
 */
public final class TitanKillRewardHandler {

    private TitanKillRewardHandler() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        ResourceLocation entityId = EntityType.getKey(dying.getType());
        TitanReward reward = TitanReward.forEntity(entityId);

        double honorGain = reward != null ? reward.honor() : 0.0;
        float combatGain = reward != null ? reward.combatPoints() : 0f;
        int currencyGain = reward != null ? reward.medals() : 0;
        int paradisRep = reward != null ? reward.paradisRep() : 0;
        int marleyRep = reward != null ? reward.marleyRep() : 0;

        // Ogre / Abnormal first-kill Honor curve in addition to standard table honor.
        if (entityId.equals(TitanReward.OGRE)) {
            honorGain += HonorData.claimFirstKillBonus(killer, entityId) ? 5.0 : 1.0;
        } else if (entityId.equals(TitanReward.ABNORMAL_TITAN) || entityId.equals(TitanReward.CRAWLING_ABNORMAL_TITAN)) {
            honorGain += HonorData.claimFirstKillBonus(killer, "abnormal_first_kill_shared") ? 5.0 : 1.0;
        }

        if (reward == null && honorGain == 0) {
            return; // not a titan this system rewards for
        }

        List<String> lines = new ArrayList<>();

        // Line 1: kill + currency (medals for Eldian, banknotes for Marley)
        CurrencyFaction.Faction faction = CurrencyFaction.get(killer);
        String currencyLabel = null;
        if (currencyGain > 0 && faction == CurrencyFaction.Faction.ELDIAN) {
            MedalData.addBalance(killer, currencyGain);
            currencyLabel = currencyGain + " Medals";
        } else if (currencyGain > 0 && faction == CurrencyFaction.Faction.MARLEY) {
            BanknoteData.addBalance(killer, currencyGain);
            currencyLabel = currencyGain + " Banknotes";
        }
        lines.add(currencyLabel != null ? "+1 Kill | " + currencyLabel : "+1 Kill");

        // Line 2: reputation
        if (paradisRep != 0) com.aotaddon.reputation.ReputationData.addParadis(killer, paradisRep);
        if (marleyRep != 0) com.aotaddon.reputation.ReputationData.addMarley(killer, marleyRep);
        if (paradisRep != 0 || marleyRep != 0) {
            StringBuilder rep = new StringBuilder("+ Reputation | ");
            if (paradisRep != 0) rep.append(paradisRep > 0 ? "+" : "").append(paradisRep);
            if (marleyRep != 0) {
                if (paradisRep != 0) rep.append(" ");
                rep.append(marleyRep > 0 ? "+" : "").append(marleyRep);
            }
            lines.add(rep.toString());
        }

        // Line 3: honor
        if (honorGain != 0) {
            HonorData.addBalance(killer, honorGain);
            lines.add("+ Honor | +" + formatNumber(honorGain));
        }

        // Line 4: combat experience
        if (combatGain != 0) {
            CombatXpData.addBalance(killer, combatGain);
            lines.add("+ Combat | +" + formatNumber(combatGain) + " Experience");
        }

        PacketDistributor.sendToPlayer(killer, new RewardPopupPayload(lines));
    }

    private static String formatNumber(double value) {
        return (value == Math.floor(value)) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Register alongside onLivingDeath: NeoForge.EVENT_BUS.addListener(TitanKillRewardHandler::onPlayerLogin); */
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        HonorData.syncOnLogin(player);
        PlayerCardSyncPayload.send(player);
        PdLifeSyncPayload.send(player);
    }
}