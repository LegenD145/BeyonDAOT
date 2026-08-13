package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the "Bastion" state for shifter titans.
 *
 * When toggled on by the player pressing the keybind:
 * - The titan cannot move (velocity zeroed every tick)
 * - The titan cannot attack or use abilities (blocked in BastionAbilityMixin)
 * - The titan regens 2x Danny's normal rate (0.476f HP per tick = 0.238 * 2)
 * - The titan's head is locked looking downward
 * - Smoke particles spawn around the titan's head area
 *
 * The player presses the same keybind to exit. No cooldown — being stationary
 * and vulnerable is the downside.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class BastionStateHandler {

    /** 2x Danny's REGEN_PER_TICK of 0.238f */
    private static final float BASTION_REGEN_PER_TICK = 0.476f;

    /** Head pitch when looking down (degrees, positive = down) */
    private static final float LOOK_DOWN_PITCH = 45.0f;

    /** All shifter player UUIDs currently in bastion state */
    private static final Set<UUID> bastionPlayers = new HashSet<>();

    // -------------------------------------------------------------------------
    // Public API — called from network packet handler when keybind fires
    // -------------------------------------------------------------------------

    public static boolean toggle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (bastionPlayers.contains(uuid)) {
            bastionPlayers.remove(uuid);
            AotAddon.LOGGER.info("[Bastion] {} exited bastion state", player.getName().getString());
            return false;
        } else {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                AotAddon.LOGGER.info("[Bastion] {} has no vehicle", player.getName().getString());
                return false;
            }
            AotAddon.LOGGER.info("[Bastion] Vehicle class: {}", vehicle.getClass().getName());
            if (!isInTitanForm(player)) {
                AotAddon.LOGGER.info("[Bastion] {} not in titan form", player.getName().getString());
                return false;
            }
            bastionPlayers.add(uuid);
            AotAddon.LOGGER.info("[Bastion] {} entered bastion state", player.getName().getString());
            return true;
        }
    }

    public static boolean isInBastion(UUID uuid) {
        return bastionPlayers.contains(uuid);
    }

    /** Called when the player dismounts or dies — clean up state */
    public static void forceExit(UUID uuid) {
        bastionPlayers.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Server tick
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (bastionPlayers.isEmpty()) return;

        Iterator<UUID> it = bastionPlayers.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);

            // Remove if player is offline or dead
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }

            // Remove if not in titan form anymore
            Entity titan = player.getVehicle();
            if (titan == null || !isTitanEntity(titan)) {
                it.remove();
                continue;
            }

            LivingEntity titanLiving = (LivingEntity) titan;

            // 1. Zero titan velocity — they cannot move
            titan.setDeltaMovement(Vec3.ZERO);

            // 2. Double regen on the titan entity
            if (titanLiving.getHealth() < titanLiving.getMaxHealth()) {
                titanLiving.heal(BASTION_REGEN_PER_TICK);
            }

            // 3. Lock head looking down
            titan.setXRot(LOOK_DOWN_PITCH);
            titanLiving.xRotO = LOOK_DOWN_PITCH;

            // 4. Smoke particles around the titan's head
            if (titan.level() instanceof ServerLevel serverLevel) {
                spawnHeadSmoke(serverLevel, titanLiving);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Particle smoke
    // -------------------------------------------------------------------------

    private static void spawnHeadSmoke(ServerLevel level, LivingEntity titan) {
        // Only spawn every 5 ticks to avoid spam
        if (level.getServer().getTickCount() % 5 != 0) return;

        double headX = titan.getX();
        double headY = titan.getY() + titan.getBbHeight() * 0.9;
        double headZ = titan.getZ();

        try {
            // Use Danny's PLAYER_DISMOUNT_PARTICLE — same smoke used during normal regen
            Class<?> dannysAot = Class.forName("daot.DannysAot");
            Object particleType = dannysAot.getField("PLAYER_DISMOUNT_PARTICLE").get(null);

            // Cast to ParticleOptions and spawn
            net.minecraft.core.particles.ParticleOptions particle =
                    (net.minecraft.core.particles.ParticleOptions) particleType;

            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() - 0.5) * titan.getBbWidth();
                double oy = (Math.random() - 0.5) * 1.5;
                double oz = (Math.random() - 0.5) * titan.getBbWidth();
                level.sendParticles(particle,
                        headX + ox, headY + oy, headZ + oz,
                        1, 0.0, 0.05, 0.0, 0.01);
            }
        } catch (Exception e) {
            // Fallback to vanilla smoke if Danny's particle not found
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    headX, headY, headZ,
                    3, titan.getBbWidth() * 0.4, 0.5, titan.getBbWidth() * 0.4, 0.0
            );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isInTitanForm(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        return vehicle != null && isTitanEntity(vehicle);
    }

    private static boolean isTitanEntity(Entity e) {
        try {
            return Class.forName("daot.TitanEntity").isInstance(e);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}