package com.aotaddon.carry;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges vanilla/NeoForge events to CarryManager. Registered automatically
 * via @EventBusSubscriber on the default (game) bus - no manual wiring
 * needed in AotAddon's constructor, same as PlayerInventoryAccessEvents.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class CarryInteractionEvents {

    // Tracks each carrier's sneak state from the previous tick, so we only
    // fire a release on the false->true transition (press), not every tick
    // sneak is held.
    private static final Map<UUID, Boolean> LAST_SNEAK_STATE = new HashMap<>();

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        var result = CarryManager.onPlayerInteract(event.getEntity(), event.getHand(), event.getTarget(), null);
        if (result.consumesAction()) {
            event.setCanceled(true);
        }
    }

    /**
     * The carried player has no dismount option of their own - only an
     * explicit carrier-initiated release (flagged via CarryManager) is let
     * through. Anything else (sneak-to-dismount, jump, etc.) is cancelled.
     */
    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isMounting()) {
            return;
        }
        if (!(event.getEntityMounting() instanceof ServerPlayer carried)) {
            return;
        }
        if (!(event.getEntityBeingMounted() instanceof ServerPlayer)) {
            return;
        }
        if (!CarryManager.isReleaseExpected(carried.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Detects the carrier pressing shift (not holding) to release whoever
     * they're carrying. Only fires for entities that currently have at least
     * one Player passenger, so this is cheap for the common case.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer carrier)) {
            return;
        }
        if (carrier.getPassengers().isEmpty()) {
            LAST_SNEAK_STATE.remove(carrier.getUUID());
            return;
        }

        boolean sneakingNow = carrier.isShiftKeyDown();
        boolean sneakingBefore = LAST_SNEAK_STATE.getOrDefault(carrier.getUUID(), false);
        LAST_SNEAK_STATE.put(carrier.getUUID(), sneakingNow);

        if (sneakingNow && !sneakingBefore) {
            CarryManager.releaseCarry(carrier);
        }
    }

    /**
     * Blocks melee attacks while carrying someone. Deliberately a SEPARATE
     * event from EntityInteract, so this has no effect on horse-mounting
     * (which goes through EntityInteract) or ODM hooks (which go through
     * DAOT's own keybind/packet pipeline, not vanilla attack/item events).
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof Player carrier && CarryManager.isCarryingAnyone(carrier)) {
            event.setCanceled(true);
        }
    }

    /**
     * Blocks generic right-click item use (eating, drinking, weapon-ability
     * right clicks) while carrying. Does NOT touch EntityInteract, so
     * mounting horses is unaffected.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (CarryManager.isCarryingAnyone(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * Catches continuous-use items (bows, shields, food) that RightClickItem
     * alone won't fully stop once use has started.
     */
    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player carrier && CarryManager.isCarryingAnyone(carrier)) {
            event.setCanceled(true);
        }
    }
}