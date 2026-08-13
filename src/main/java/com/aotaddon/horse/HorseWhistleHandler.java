package com.aotaddon.horse;

import com.aotaddon.registry.ModAttachments;
import com.aotaddon.util.CombatTagHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Horse whistle system.
 *
 * Bonding: empty-hand right-click on your own tamed horse bonds it (does not
 * interfere with the normal mount interaction — we never cancel the event).
 *
 * Summon: keybind-triggered C2S packet (see HorseWhistlePayload) teleports
 * the bonded horse to the player, subject to combat tag and cooldown checks.
 */
public class HorseWhistleHandler {

    private static final long SUMMON_COOLDOWN_TICKS = 3600L; // 180s * 20 ticks/sec

    // -------------------------------------------------------------------
    // Bonding
    // -------------------------------------------------------------------

    /**
     * Registered via NeoForge.EVENT_BUS.addListener(HorseWhistleHandler::onEntityInteract)
     * in AotAddon.
     */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Entity target = event.getTarget();
        if (!(target instanceof AbstractHorse horse)) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().isEmpty()) return;

        if (!horse.isTamed() || !player.getUUID().equals(horse.getOwnerUUID())) {
            player.displayClientMessage(
                    Component.literal("You don't own this horse.").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        HorseBondData data = player.getData(ModAttachments.HORSE_BOND);
        data.horseUUID = horse.getUUID();
        data.dimension = player.level().dimension();

        player.displayClientMessage(
                Component.literal("Horse bonded.").withStyle(ChatFormatting.GREEN),
                true);
    }

    // -------------------------------------------------------------------
    // Summon
    // -------------------------------------------------------------------

    /**
     * Called from HorseWhistlePayload's server handler when the player
     * presses the whistle keybind.
     */
    public static void onSummon(ServerPlayer player) {
        if (CombatTagHandler.isInCombat(player)) {
            player.displayClientMessage(
                    Component.literal("In combat, please try again later.").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        HorseBondData data = player.getData(ModAttachments.HORSE_BOND);

        if (data.horseUUID == null || data.dimension == null
                || !data.dimension.equals(player.level().dimension())) {
            player.displayClientMessage(
                    Component.literal("You have no horse bonded to you.").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        Entity found = player.serverLevel().getEntity(data.horseUUID);
        if (!(found instanceof AbstractHorse horse) || !horse.isAlive()) {
            player.displayClientMessage(
                    Component.literal("You have no horse bonded to you.").withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        // Cooldown — fails silently for now (not surfaced as a message yet).
        long currentTick = player.level().getGameTime();
        if (currentTick < data.cooldownExpiryTick) {
            return;
        }

        horse.teleportTo(player.getX(), player.getY(), player.getZ());
        data.cooldownExpiryTick = currentTick + SUMMON_COOLDOWN_TICKS;
    }
}