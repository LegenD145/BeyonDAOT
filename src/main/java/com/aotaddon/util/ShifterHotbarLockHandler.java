package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * While a player is riding (piloting) a shifter titan body, item use/attack
 * still fires normally through the hotbar - a real problem once items like
 * guns are involved, since you could be firing a gun while also fighting as
 * a titan.
 *
 * Slot-switching itself (scroll/number keys) is blocked separately in
 * ShifterHotbarSlotLockMixin, at the packet level - no tick cost. This class
 * only handles the second half: stopping the one already-selected item from
 * actually being used. These are already event-driven (no polling needed),
 * so no tick listener here either.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class ShifterHotbarLockHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (aotaddon$isRidingShifter(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (aotaddon$isRidingShifter(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (aotaddon$isRidingShifter(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (aotaddon$isRidingShifter(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && aotaddon$isRidingShifter(player)) {
            event.setCanceled(true);
        }
    }

    private static boolean aotaddon$isRidingShifter(Entity player) {
        Entity vehicle = player.getVehicle();
        return vehicle != null && ShifterTitanUtil.isShifterTitan(vehicle);
    }
}