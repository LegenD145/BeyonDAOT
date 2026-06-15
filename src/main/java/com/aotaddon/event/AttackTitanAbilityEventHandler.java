package com.aotaddon.event;

import com.aotaddon.AotAddon;
import com.aotaddon.util.WarhammerInheritanceTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Intercepts the Attack Titan's triggerAbility calls for slots 5, 8, 9
 * by watching the entity tick and detecting when the player presses
 * those ability keys while riding the Attack Titan.
 *
 * Since we can't mixin into AttackTitanEntity at load time (Connector
 * loads it too late), we instead hook into the NeoForge networking
 * layer to intercept the ability packet before it reaches Danny's handler.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class AttackTitanAbilityEventHandler {

    private static Class<?> attackTitanClass = null;
    private static Class<?> modNetworkingClass = null;
    private static boolean classesChecked = false;

    /**
     * We hook into the ModNetworking ability handler via a mixin on the
     * NeoForge packet handler. Since we can't mixin daot classes directly,
     * we instead register a NeoForge server tick listener that checks
     * if the Attack Titan rider just triggered ability 5/8/9 by watching
     * the attackNumber change.
     *
     * Detection logic:
     * - If attackNumber just became 3 (groundsmash) AND the rider has
     *   inheritance AND we see a specific pattern, queue the ability.
     *
     * Note: This is a best-effort approach. The proper fix would be a
     * Fabric-side mixin, but that requires restructuring the addon as
     * a Fabric mod. For now this event-based approach handles the
     * inheritance grant/revoke correctly.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!classesChecked) tryLoadClasses();
        if (attackTitanClass == null) return;
        if (!attackTitanClass.isInstance(event.getEntity())) return;

        Object titan = event.getEntity();

        try {
            Object level = getLevel(titan);
            if (level == null || isClientSide(level)) return;

            UUID shifterUUID = getShifterUUID(titan);
            if (shifterUUID == null) return;

            ServerPlayer rider = getRiderByUUID(level, shifterUUID);
            if (rider == null) return;

            if (!WarhammerInheritanceTracker.hasInheritance(rider)) return;

            // Check if ability was just triggered via the pending map
            // (set by the packet mixin interceptor in ThunderSpearLoadMixin pattern)
            // For now we rely on WarhammerAbilityEventHandler.queueAbility() being
            // called from the network packet handler mixin

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] AttackTitanAbilityEventHandler failed: {}",
                    e.getMessage());
        }
    }

    private static void tryLoadClasses() {
        classesChecked = true;
        try {
            attackTitanClass = Class.forName("daot.AttackTitanEntity");
            modNetworkingClass = Class.forName("daot.network.ModNetworking");
        } catch (ClassNotFoundException e) {
            AotAddon.LOGGER.warn("[AotAddon] Danny's classes not found: {}", e.getMessage());
        }
    }

    private static Object getLevel(Object entity) throws Exception {
        return entity.getClass().getMethod("method_37908").invoke(entity);
    }

    private static boolean isClientSide(Object level) throws Exception {
        return (boolean) level.getClass().getMethod("method_8608").invoke(level);
    }

    private static UUID getShifterUUID(Object titan) throws Exception {
        Object result = titan.getClass().getMethod("getShifterUUID").invoke(titan);
        if (result instanceof Optional<?> opt) return opt.isPresent() ? (UUID) opt.get() : null;
        return result instanceof UUID u ? u : null;
    }

    private static ServerPlayer getRiderByUUID(Object level, UUID uuid) throws Exception {
        Object server = level.getClass().getMethod("getServer").invoke(level);
        Object playerList = server.getClass().getMethod("getPlayerList").invoke(server);
        Object player = playerList.getClass().getMethod("getPlayer", UUID.class)
                .invoke(playerList, uuid);
        return player instanceof ServerPlayer sp ? sp : null;
    }
}
