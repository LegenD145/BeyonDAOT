package com.aotaddon.reputation;

import com.aotaddon.currency.CurrencyFaction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Awards reputation on PvP kills based on the victim's bloodline faction.
 * Titan kill rep is handled in TitanKillRewardHandler.
 */
public class ReputationHandler {

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        CurrencyFaction.Faction victimFaction = CurrencyFaction.get(victim);

        if (victimFaction == CurrencyFaction.Faction.MARLEY) {
            ReputationData.addParadis(killer, 2);
            ReputationData.addMarley(killer, -1);
        } else if (victimFaction == CurrencyFaction.Faction.ELDIAN) {
            ReputationData.addMarley(killer, 2);
            ReputationData.addParadis(killer, -1);
        }
    }
}
