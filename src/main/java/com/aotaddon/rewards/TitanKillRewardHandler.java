package com.aotaddon.rewards;

import com.aotaddon.currency.BanknoteData;
import com.aotaddon.currency.CurrencyFaction;
import com.aotaddon.currency.MedalData;
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
 *   - Honor Points: awarded to any killer, via HonorData (persistentData, matches
 *     MedalData/BanknoteData style).
 *   - Medals/Banknotes: routed by CurrencyFaction — Eldian killers get Medals via
 *     MedalData, Marley killers get Banknotes via BanknoteData, NONE gets neither
 *     (currency skipped, other lines still show).
 *   - Reputation: NOT wired to a real system yet — reputation.js on the KubeJS
 *     side is still the source of truth for rep. This only shows the number in
 *     the popup text; it does NOT write reputation anywhere. Wire this up once
 *     reputation is ported to Java, using whatever persistentData key
 *     reputation.js already uses (don't invent a new key here).
 *   - Military branch (Scout/Garrison/MP) gating: NOT implemented — that system
 *     doesn't exist in Java yet either. Everyone eligible by faction currently
 *     gets the full medal/banknote amount from the table regardless of branch.
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

        // Ogre / Abnormal first-kill Honor curve (in addition to any standard table honor, though
        // Ogre currently has no standard table row — see TitanReward's TODO).
        if (entityId.equals(TitanReward.OGRE)) {
            honorGain += HonorData.claimFirstKillBonus(killer, entityId) ? 5.0 : 1.0;
        } else if (entityId.equals(TitanReward.ABNORMAL_TITAN) || entityId.equals(TitanReward.CRAWLING_ABNORMAL_TITAN)) {
            honorGain += HonorData.claimFirstKillBonus(killer, "abnormal_first_kill_shared") ? 5.0 : 1.0;
        }

        if (reward == null && honorGain == 0) {
            return; // not a titan this system rewards for
        }

        List<String> lines = new ArrayList<>();

        // Line 1: kill + currency (Medals for Eldian, Banknotes for Marley, skipped for NONE)
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

        // Line 2: reputation (display only for now — see class javadoc)
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

        // Line 4: combat experience (display only — combat/body-stat system not built yet)
        if (combatGain != 0) {
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
    }
}