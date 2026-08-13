package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Shiftlock: while toggled on and the player is riding a shifter titan body,
 * forces the titan's yaw (and body/head rotation) to match the player's camera
 * yaw every server tick — regardless of titan type, armed/M2 state, or whether
 * that titan type even has its own camera-lock stance.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class ShiftlockStateHandler {

    private static final Set<UUID> shiftlockPlayers = new HashSet<>();

    /** Result of a toggle attempt, so the payload handler can show an accurate message. */
    public enum ToggleResult {
        ENABLED,
        DISABLED,
        NOT_IN_TITAN_FORM
    }

    // -------------------------------------------------------------------------
    // Public API — called from network packet handler when keybind fires
    // -------------------------------------------------------------------------

    public static ToggleResult toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (shiftlockPlayers.contains(uuid)) {
            shiftlockPlayers.remove(uuid);
            AotAddon.LOGGER.info("[Shiftlock] {} disabled shiftlock", player.getName().getString());
            return ToggleResult.DISABLED;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            AotAddon.LOGGER.info("[Shiftlock] {} has no vehicle", player.getName().getString());
            return ToggleResult.NOT_IN_TITAN_FORM;
        }

        AotAddon.LOGGER.info("[Shiftlock] Vehicle class: {}", vehicle.getClass().getName());

        if (!isTitanEntity(vehicle)) {
            AotAddon.LOGGER.info("[Shiftlock] {} not in titan form (vehicle={})",
                    player.getName().getString(), vehicle.getClass().getName());
            return ToggleResult.NOT_IN_TITAN_FORM;
        }

        shiftlockPlayers.add(uuid);
        AotAddon.LOGGER.info("[Shiftlock] {} enabled shiftlock", player.getName().getString());
        return ToggleResult.ENABLED;
    }

    public static boolean isInShiftlock(UUID uuid) {
        return shiftlockPlayers.contains(uuid);
    }

    /** Called when the player dismounts or dies — clean up state */
    public static void forceExit(UUID uuid) {
        shiftlockPlayers.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (shiftlockPlayers.isEmpty()) return;

        Iterator<UUID> it = shiftlockPlayers.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);

            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }

            Entity titan = player.getVehicle();
            if (titan == null || !isTitanEntity(titan)) {
                it.remove();
                continue;
            }

            LivingEntity titanLiving = (LivingEntity) titan;
            float camYaw = player.getYRot();

            titan.setYRot(camYaw);
            titan.yRotO = camYaw;
            titanLiving.yBodyRot = camYaw;
            titanLiving.yBodyRotO = camYaw;
            titanLiving.yHeadRot = camYaw;
            titanLiving.yHeadRotO = camYaw;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isTitanEntity(Entity e) {
        try {
            return Class.forName("daot.ShifterTitan").isInstance(e);
        } catch (ClassNotFoundException ex) {
            AotAddon.LOGGER.warn("[Shiftlock] daot.ShifterTitan class not found via reflection!", ex);
            return false;
        }
    }
}