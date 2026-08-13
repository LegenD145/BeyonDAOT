package com.aotaddon.gascanister;

import com.aotaddon.AotAddon;
import com.aotaddon.gas.DaotGasReflection;
import com.aotaddon.gas.GasTierText;
import com.aotaddon.util.CombatTagHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Passively refills a crouching, out-of-combat player's ODM gear from a nearby
 * loaded GasCanisterBlockEntity. Mirrors CampfireRegenHandler's proximity-scan
 * pattern exactly.
 *
 * Overfill: if the player's gear is already at max and they keep crouching next
 * to a canister that still has gas, their gear "explodes" (small knockback/visual
 * explosion + guaranteed ~2 hearts of direct damage) once per continuous
 * overfill-attempt session - not spammed every check interval.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class GasCanisterPassiveRefillHandler {

    private static final int CHECK_INTERVAL_TICKS = 10; // 0.5s
    private static final int SEARCH_RADIUS = 2;          // "VERY close" per spec
    private static final int TRANSFER_PER_CHECK = 15;    // ~30 gas/sec while topping up

    private static final float OVERFILL_EXPLOSION_POWER = 0.6f;
    private static final float OVERFILL_DAMAGE = 4.0f;   // ~2 hearts

    /** Players who've already been punished for the current continuous overfill attempt. */
    private static final Set<UUID> alreadyPunishedThisSession = new HashSet<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CombatTagHandler.isInCombat(player)) {
                alreadyPunishedThisSession.remove(player.getUUID());
                continue;
            }
            if (!player.isCrouching()) {
                alreadyPunishedThisSession.remove(player.getUUID());
                continue;
            }

            GasCanisterBlockEntity canister = findNearbyCanister(player);
            if (canister == null) {
                alreadyPunishedThisSession.remove(player.getUUID());
                continue;
            }

            ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
            if (legs.isEmpty() || !DaotGasReflection.isODMGear(legs.getItem())) {
                alreadyPunishedThisSession.remove(player.getUUID());
                continue;
            }

            int gearGas = DaotGasReflection.getGas(legs);
            int gearMax = DaotGasReflection.getMaxGas(legs);
            if (gearGas < 0 || gearMax <= 0) continue;

            // Dialogue: reuse the same tiered flavor text from the tap-to-check keybind,
            // shown passively here since this is the replacement for the removed gas bar.
            String tierText = GasTierText.resolve(gearGas, gearMax);
            if (tierText != null) {
                player.displayClientMessage(Component.literal(tierText), true);
            }

            if (gearGas >= gearMax) {
                // Already full but still trying - overfill punishment (once per session).
                if (canister.getStoredGas() > 0 && !alreadyPunishedThisSession.contains(player.getUUID())) {
                    triggerOverfillExplosion(player);
                    alreadyPunishedThisSession.add(player.getUUID());
                }
                continue;
            }
            // Normal refill: pull from the canister, capped by both gear headroom and
            // the canister's own remaining stock.
            int missing = gearMax - gearGas;
            int wanted = Math.min(TRANSFER_PER_CHECK, missing);
            int actuallyTaken = canister.removeGas(wanted);
            if (actuallyTaken > 0) {
                DaotGasReflection.setGas(legs, Math.min(gearMax, gearGas + actuallyTaken));
            }
        }
    }

    private static void triggerOverfillExplosion(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Visual/knockback only - NONE interaction means no block or terrain damage.
        level.explode(player, player.getX(), player.getY(), player.getZ(),
                OVERFILL_EXPLOSION_POWER, Level.ExplosionInteraction.NONE);

        // Guaranteed damage rather than relying on explosion falloff, since the
        // player is standing right at the epicenter but explosion damage can still
        // vary with armor/enchants/distance rounding. Using a plain generic damage
        // source rather than guessing DamageSources#explosion(...)'s exact overload.
        player.hurt(player.damageSources().generic(), OVERFILL_DAMAGE);

        // The gear itself is destroyed by the over-pressurization - clear the LEGS
        // slot entirely rather than just damaging the player.
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);

        AotAddon.LOGGER.info("[GasCanister] {} overfilled their ODM gear, took damage, and lost their gear",
                player.getName().getString());
    }

    private static GasCanisterBlockEntity findNearbyCanister(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof GasCanisterBlockEntity canister) {
                        return canister;
                    }
                }
            }
        }
        return null;
    }
}