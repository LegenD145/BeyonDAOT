package com.aotaddon.event;

import com.aotaddon.AotAddon;
import com.aotaddon.util.WarhammerInheritanceTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Uses NeoForge's EntityTickEvent to hook into AttackTitanEntity.tickEat()
 * at runtime, AFTER Sinytra Connector has fully loaded Danny's mod.
 *
 * This avoids the "loaded too early" mixin crash entirely.
 *
 * Detects:
 * 1. Attack Titan eating a Warhammer shifter player -> grant inheritance
 * 2. Any player with inheritance being eaten -> revoke inheritance
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class WarhammerInheritanceEventHandler {

    // Cache the AttackTitanEntity class once found to avoid repeated reflection
    private static Class<?> attackTitanClass = null;
    private static Class<?> warhammerTitanClass = null;
    private static boolean classesChecked = false;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        // Lazy-load Danny's classes after Connector has initialized them
        if (!classesChecked) {
            tryLoadClasses();
        }
        if (attackTitanClass == null) return;

        // Only process AttackTitanEntity instances
        if (!attackTitanClass.isInstance(event.getEntity())) return;

        Object titan = event.getEntity();

        try {
            // Only act during eat phase (grabPhase == 4) at tick 30
            int grabPhase = getGrabPhase(titan);
            if (grabPhase != 4) return;

            int eatTicks = getEatTicks(titan);
            if (eatTicks != 30) return;

            // Get the grabbed entity
            int grabbedId = getGrabbedEntityId(titan);
            if (grabbedId == -1) return;

            Object level = getLevel(titan);
            if (level == null || isClientSide(level)) return;

            Object grabbed = getEntityById(level, grabbedId);
            if (!(grabbed instanceof ServerPlayer grabbedPlayer)) return;

            // Case 1: grabbed player is riding a Warhammer Titan -> grant inheritance
            Object vehicle = getVehicle(grabbed);
            if (vehicle != null && warhammerTitanClass != null
                    && warhammerTitanClass.isInstance(vehicle)) {

                UUID shifterUUID = getShifterUUID(titan);
                if (shifterUUID != null) {
                    ServerPlayer attackRider = getRiderByUUID(level, shifterUUID);
                    if (attackRider != null
                            && !WarhammerInheritanceTracker.hasInheritance(attackRider)) {
                        WarhammerInheritanceTracker.grantInheritance(attackRider);
                        attackRider.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                "You have inherited the power of the Warhammer Titan!"
                            ).withStyle(net.minecraft.ChatFormatting.GOLD),
                            false
                        );
                    }
                }
            }

            // Case 2: grabbed player has inheritance -> revoke it
            if (WarhammerInheritanceTracker.hasInheritance(grabbedPlayer)) {
                WarhammerInheritanceTracker.revokeInheritance(grabbedPlayer);
                grabbedPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "Your Warhammer inheritance has been lost!"
                    ).withStyle(net.minecraft.ChatFormatting.RED),
                    false
                );
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerInheritanceEventHandler tick failed: {}",
                    e.getMessage());
        }
    }

    private static void tryLoadClasses() {
        classesChecked = true;
        try {
            attackTitanClass = Class.forName("daot.AttackTitanEntity");
            AotAddon.LOGGER.info("[AotAddon] Successfully found daot.AttackTitanEntity");
        } catch (ClassNotFoundException e) {
            AotAddon.LOGGER.warn("[AotAddon] daot.AttackTitanEntity not found - Danny's AoT may not be loaded");
        }
        try {
            warhammerTitanClass = Class.forName("daot.WarhammerTitanEntity");
        } catch (ClassNotFoundException e) {
            AotAddon.LOGGER.warn("[AotAddon] daot.WarhammerTitanEntity not found");
        }
    }

    // =========================================================================
    // REFLECTION HELPERS
    // =========================================================================

    private static int getGrabPhase(Object titan) throws Exception {
        Method m = titan.getClass().getMethod("getGrabPhase");
        return (int) m.invoke(titan);
    }

    private static int getEatTicks(Object titan) throws Exception {
        // Walk class hierarchy to find eatTicks field
        Class<?> cls = titan.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField("eatTicks");
                f.setAccessible(true);
                return (int) f.get(titan);
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        return -1;
    }

    private static int getGrabbedEntityId(Object titan) throws Exception {
        Method m = titan.getClass().getMethod("getGrabbedEntityId");
        return (int) m.invoke(titan);
    }

    private static Object getLevel(Object entity) throws Exception {
        Method m = entity.getClass().getMethod("method_37908");
        return m.invoke(entity);
    }

    private static boolean isClientSide(Object level) throws Exception {
        Method m = level.getClass().getMethod("method_8608");
        return (boolean) m.invoke(level);
    }

    private static Object getEntityById(Object level, int id) throws Exception {
        Method m = level.getClass().getMethod("method_8469", int.class);
        return m.invoke(level, id);
    }

    private static Object getVehicle(Object entity) throws Exception {
        Method m = entity.getClass().getMethod("method_5854");
        return m.invoke(entity);
    }

    private static UUID getShifterUUID(Object titan) throws Exception {
        Method m = titan.getClass().getMethod("getShifterUUID");
        Object result = m.invoke(titan);
        if (result instanceof Optional<?> opt) {
            return opt.isPresent() ? (UUID) opt.get() : null;
        }
        return result instanceof UUID u ? u : null;
    }

    private static ServerPlayer getRiderByUUID(Object level, UUID uuid) throws Exception {
        Method getServer = level.getClass().getMethod("getServer");
        Object server = getServer.invoke(level);
        Method getPlayerList = server.getClass().getMethod("getPlayerList");
        Object playerList = getPlayerList.invoke(server);
        Method getPlayer = playerList.getClass().getMethod("getPlayer", UUID.class);
        Object player = getPlayer.invoke(playerList, uuid);
        return player instanceof ServerPlayer sp ? sp : null;
    }
}
