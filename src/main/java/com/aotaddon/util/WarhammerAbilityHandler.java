package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarhammerAbilityHandler {

    private static final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final int COOLDOWN_TICKS = 60;

    // Cached spike reflection
    private static Class<?> spikeClass = null;
    private static Object spikeEntityType = null;
    private static Method setSpikeProperties = null;
    private static Method setSpawnDelay = null;
    private static Method setOwnerUUID = null;
    private static boolean spikeReflectionFailed = false;

    // Cached Fabric intermediary methods
    private static Method method_5814 = null; // setPos on spike
    private static Method method_8649 = null; // addFreshEntity on level

    public static void handleAbility(ServerPlayer player, Object attackTitan, int slot) {
        try {
            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();

            Long readyAt = cooldowns.get(player.getUUID());
            if (readyAt != null && now < readyAt) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cAbility on cooldown."),
                        true
                );
                return;
            }

            // Get Fabric-mapped level from the titan for method_8649
            Method getLevelMethod = findMethod(attackTitan.getClass(), "method_37908");
            if (getLevelMethod == null) {
                AotAddon.LOGGER.error("[AotAddon] Could not find method_37908 on titan");
                return;
            }
            Object fabricLevel = getLevelMethod.invoke(attackTitan);

            triggerAnim(attackTitan);

            switch (slot) {
                case 5 -> doGroundStomp(player, attackTitan, level);
                case 8 -> doSpikeField(player, attackTitan, fabricLevel, level);
                case 9 -> doPiercingThorns(player, attackTitan, fabricLevel, level);
            }

            drainStamina(player.getUUID(), 10.0f);
            cooldowns.put(player.getUUID(), now + COOLDOWN_TICKS);

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] handleAbility slot {}: {}", slot, e.getMessage());
        }
    }

    // ── Animation ─────────────────────────────────────────────────────────

    private static void triggerAnim(Object titan) {
        try {
            invokeMethod(titan.getClass(), titan, "setAttackNumber", new Class[]{int.class}, 3);
            invokeMethod(titan.getClass(), titan, "setAttacking", new Class[]{boolean.class}, true);
            setField(titan.getClass(), titan, "attackAnimationTicks", 35);
            setField(titan.getClass(), titan, "attackCooldown", 35);
            setField(titan.getClass(), titan, "attackEffectTimer", 0);
            setField(titan.getClass(), titan, "attackEffectTriggered", false);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] triggerAnim failed: {}", e.getMessage());
        }
    }

    // ── Slot 5: Ground Stomp ──────────────────────────────────────────────

    private static void doGroundStomp(ServerPlayer player, Object titan, ServerLevel level) {
        Entity titanEntity = (Entity) titan;
        double cx = titanEntity.getX();
        double cy = titanEntity.getY();
        double cz = titanEntity.getZ();
        float yaw = titanEntity.getYRot();
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ =  Math.cos(yawRad);
        double radius = 8.0;

        AABB box = new AABB(cx - radius, cy - 2, cz - radius, cx + radius, cy + 6, cz + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (target == titan || target.getVehicle() == titan) continue;
            if (target instanceof ServerPlayer sp && sp.getUUID().equals(player.getUUID())) continue;

            double dx = target.getX() - cx;
            double dz = target.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius) continue;

            double dot = (dx * fwdX + dz * fwdZ) / (dist + 0.001);
            if (dot < 0.0) continue;

            target.hurt(level.damageSources().mobAttack((LivingEntity) titan), 15.0f);
            double strength = (1.0 - dist / radius) * 2.5;
            target.setDeltaMovement(
                    target.getDeltaMovement().add(fwdX * strength, 0.6 + strength * 0.3, fwdZ * strength)
            );
            target.hurtMarked = true;
        }

        level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.6f);
        AotAddon.LOGGER.info("[AotAddon] Ground Stomp fired");
    }

    // ── Slot 8: Spike Field ───────────────────────────────────────────────

    private static void doSpikeField(ServerPlayer player, Object titan, Object fabricLevel, ServerLevel level) {
        if (!initSpikeReflection(fabricLevel)) return;

        Entity titanEntity = (Entity) titan;
        double cx = titanEntity.getX();
        double cy = titanEntity.getY();
        double cz = titanEntity.getZ();
        int groundY = (int) Math.floor(cy);

        try {
            int spikeCount = 10;
            for (int i = 0; i < spikeCount; i++) {
                double angle = Math.PI * 2 * i / (double) spikeCount;
                double spikeX = cx + Math.cos(angle) * 6.0;
                double spikeZ = cz + Math.sin(angle) * 6.0;

                int surfaceY = findSurface(level, spikeX, groundY, spikeZ);

                Object spike = makeSpike(fabricLevel, spikeX, surfaceY, spikeZ,
                        0.0f, 0.0f, 9.0f, 2.0f, i * 2, player.getUUID());
                if (spike == null) continue;

                method_8649.invoke(fabricLevel, (Entity) spike);
            }

            level.playSound(null, cx, cy, cz, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 2.0f, 0.5f);
            AotAddon.LOGGER.info("[AotAddon] Spike Field fired");

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] doSpikeField error: {}", e.getMessage());
        }
    }

    // ── Slot 9: Piercing Thorns ───────────────────────────────────────────

    private static void doPiercingThorns(ServerPlayer player, Object titan, Object fabricLevel, ServerLevel level) {
        if (!initSpikeReflection(fabricLevel)) return;

        Entity titanEntity = (Entity) titan;
        double cx = titanEntity.getX();
        double cy = titanEntity.getY();
        double cz = titanEntity.getZ();
        float yaw = titanEntity.getYRot();
        double yawRad = Math.toRadians(yaw);
        double fwdX = -Math.sin(yawRad);
        double fwdZ =  Math.cos(yawRad);
        int groundY = (int) Math.floor(cy);

        try {
            // 4 spikes in forward fan
            double[] offsets = { -20, -6, 6, 20 };
            for (int i = 0; i < offsets.length; i++) {
                double a = yawRad + Math.toRadians(offsets[i]);
                double spikeX = cx + (-Math.sin(a)) * 7.0;
                double spikeZ = cz + ( Math.cos(a)) * 7.0;

                int surfaceY = findSurface(level, spikeX, groundY, spikeZ);

                Object spike = makeSpike(fabricLevel, spikeX, surfaceY, spikeZ,
                        0.0f, 0.0f, 10.0f, 1.8f, i * 3, player.getUUID());
                if (spike == null) continue;

                method_8649.invoke(fabricLevel, (Entity) spike);
            }

            // Direct damage in forward cone
            AABB box = new AABB(cx - 5, cy - 1, cz - 5, cx + 5, cy + 8, cz + 5);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (target == titan || target.getVehicle() == titan) continue;
                if (target instanceof ServerPlayer sp && sp.getUUID().equals(player.getUUID())) continue;

                double dx = target.getX() - cx;
                double dz = target.getZ() - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 5.0) continue;

                double dot = (dx * fwdX + dz * fwdZ) / (dist + 0.001);
                if (dot < 0.3) continue;

                target.hurt(level.damageSources().mobAttack((LivingEntity) titan), 20.0f);
            }

            level.playSound(null, cx, cy, cz, SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 2.0f, 0.7f);
            AotAddon.LOGGER.info("[AotAddon] Piercing Thorns fired");

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] doPiercingThorns error: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static int findSurface(ServerLevel level, double x, int groundY, double z) {
        for (int dy = 10; dy >= -10; --dy) {
            BlockPos check = new BlockPos((int) Math.floor(x), groundY + dy, (int) Math.floor(z));
            if (level.getBlockState(check).isSolid() && !level.getBlockState(check.above()).isSolid()) {
                return groundY + dy + 1;
            }
        }
        return groundY;
    }

    private static Object makeSpike(Object fabricLevel, double x, double y, double z,
                                    float tiltX, float tiltZ, float height, float width,
                                    int spawnDelay, UUID ownerUUID) {
        try {
            Object spike = spikeClass
                    .getDeclaredConstructor(EntityType.class, Level.class)
                    .newInstance(spikeEntityType, fabricLevel);

            method_5814.invoke(spike, x, y, z);
            setSpikeProperties.invoke(spike, tiltX, tiltZ, height, width);
            setSpawnDelay.invoke(spike, spawnDelay);
            setOwnerUUID.invoke(spike, ownerUUID);
            return spike;
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] makeSpike error: {}", e.getMessage());
            return null;
        }
    }

    private static boolean initSpikeReflection(Object fabricLevel) {
        if (spikeReflectionFailed) return false;
        if (spikeClass != null) return true;

        try {
            spikeClass = Class.forName("daot.WarhammerSpikeEntity");
            Class<?> dannysAot = Class.forName("daot.DannysAot");
            spikeEntityType = dannysAot.getField("WARHAMMER_SPIKE").get(null);

            setSpikeProperties = spikeClass.getMethod("setSpikeProperties",
                    float.class, float.class, float.class, float.class);
            setSpawnDelay = spikeClass.getMethod("setSpawnDelay", int.class);
            setOwnerUUID  = spikeClass.getMethod("setOwnerUUID", UUID.class);

            method_5814 = findMethod(spikeClass, "method_5814",
                    double.class, double.class, double.class);
            if (method_5814 == null) throw new NoSuchMethodException("method_5814 not found");

            method_8649 = findMethod(fabricLevel.getClass(), "method_8649",
                    net.minecraft.world.entity.Entity.class);
            if (method_8649 == null) throw new NoSuchMethodException("method_8649 not found");

            AotAddon.LOGGER.info("[AotAddon] Spike reflection initialized.");
            return true;

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] Spike reflection init failed: {}", e.getMessage());
            spikeReflectionFailed = true;
            return false;
        }
    }

    private static void drainStamina(UUID uuid, float amount) {
        try {
            Class<?> networking = Class.forName("daot.network.ModNetworking");
            networking.getMethod("drainStamina", UUID.class, float.class).invoke(null, uuid, amount);
        } catch (Exception e) {
            // Non-fatal
        }
    }

    private static void invokeMethod(Class<?> startClass, Object instance, String name, Class<?>[] params, Object... args) throws Exception {
        Class<?> c = startClass;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                m.invoke(instance, args);
                return;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Could not find " + name + " in " + startClass.getName());
    }

    private static Method findMethod(Class<?> startClass, String name, Class<?>... params) {
        Class<?> c = startClass;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void setField(Class<?> startClass, Object instance, String name, Object value) throws Exception {
        Class<?> c = startClass;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(instance, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Could not find field " + name + " in " + startClass.getName());
    }
}