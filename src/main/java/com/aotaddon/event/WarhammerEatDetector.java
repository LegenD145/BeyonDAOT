package com.aotaddon.event;

import com.aotaddon.AotAddon;
import com.aotaddon.util.WarhammerInheritanceTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Detects when an Attack Titan rider eats a Warhammer shifter player.
 *
 * Danny tags Warhammer shifter players with the scoreboard tag "warhammer"
 * so we use that instead of reflecting into WarhammerTitanEntity — much simpler.
 *
 * Fires at eatTicks == 29, one tick before Danny's damage lands, so the
 * grabbed entity is still alive and reachable.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class WarhammerEatDetector {

    private static final Set<UUID> processedEats = new HashSet<>();

    // Cached reflection — resolved once on first tick, reused forever
    private static Class<?> attackTitanClass = null;
    private static Method getGrabPhase = null;
    private static Method getGrabbedEntityId = null;
    private static Method getShifterUUID = null;
    private static Field eatTicksField = null;
    private static boolean reflectionFailed = false;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (!reflectionFailed && attackTitanClass == null) {
            if (!initReflection()) return;
        }
        if (reflectionFailed) return;

        level.getEntities().getAll().forEach(entity -> {
            try {
                if (!attackTitanClass.isInstance(entity)) return;

                int grabPhase = (Integer) getGrabPhase.invoke(entity);
                if (grabPhase != 4) {
                    processedEats.remove(entity.getUUID());
                    return;
                }

                UUID titanId = entity.getUUID();
                if (processedEats.contains(titanId)) return;

                int eatTicks = eatTicksField.getInt(entity);
                if (eatTicks != 29) return;

                processedEats.add(titanId);

                // Get the grabbed entity
                int grabbedId = (Integer) getGrabbedEntityId.invoke(entity);
                if (grabbedId == -1) return;

                net.minecraft.world.entity.Entity grabbed = level.getEntity(grabbedId);
                if (!(grabbed instanceof ServerPlayer eatenPlayer)) return;

                // Simple tag check — Danny tags all warhammer shifters with "warhammer"
                if (!eatenPlayer.getTags().contains("warhammer")) return;

                // Find the Attack Titan's rider
                UUID atkShifterUUID = (UUID) getShifterUUID.invoke(entity);
                if (atkShifterUUID == null) return;

                for (net.minecraft.world.entity.Entity passenger : entity.getPassengers()) {
                    if (passenger instanceof ServerPlayer rider && rider.getUUID().equals(atkShifterUUID)) {
                        WarhammerInheritanceTracker.grantInheritance(rider);
                        AotAddon.LOGGER.info("[AotAddon] Granted Warhammer inheritance to {}", rider.getName().getString());
                        break;
                    }
                }

            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] WarhammerEatDetector tick error: {}", e.getMessage());
            }
        });
    }

    private static boolean initReflection() {
        try {
            attackTitanClass   = Class.forName("daot.AttackTitanEntity");

            getGrabPhase       = attackTitanClass.getMethod("getGrabPhase");
            getGrabbedEntityId = attackTitanClass.getMethod("getGrabbedEntityId");
            getShifterUUID     = attackTitanClass.getMethod("getShifterUUID");

            Class<?> c = attackTitanClass;
            while (c != null) {
                try {
                    eatTicksField = c.getDeclaredField("eatTicks");
                    eatTicksField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (eatTicksField == null) throw new NoSuchFieldException("eatTicks not found");

            AotAddon.LOGGER.info("[AotAddon] WarhammerEatDetector reflection initialized.");
            return true;

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerEatDetector reflection init failed: {}", e.getMessage());
            reflectionFailed = true;
            return false;
        }
    }
}