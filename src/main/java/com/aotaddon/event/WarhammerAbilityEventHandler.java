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
import java.util.Random;
import java.util.UUID;

/**
 * Uses NeoForge's EntityTickEvent to hook into AttackTitanEntity
 * and add Warhammer inherited abilities to slots 5, 8, 9.
 *
 * Reads the pending ability from a per-entity field we inject via
 * a simple companion map, then fires the ability at the correct
 * attack effect tick (tick 14 of the groundsmash animation).
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class WarhammerAbilityEventHandler {

    private static Class<?> attackTitanClass = null;
    private static boolean classChecked = false;

    // Maps entity ID -> pending warhammer ability (1=stomp, 2=spike, 3=thorns)
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Integer>
            PENDING_ABILITY = new java.util.concurrent.ConcurrentHashMap<>();

    // Maps entity ID -> whether effect has already fired this attack
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean>
            EFFECT_FIRED = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Called every tick for every entity.
     * We intercept AttackTitanEntity ticks to:
     * 1. Detect when ability slots 5/8/9 are triggered
     * 2. Fire the Warhammer damage at the correct effect tick
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!classChecked) tryLoadClass();
        if (attackTitanClass == null) return;
        if (!attackTitanClass.isInstance(event.getEntity())) return;

        Object titan = event.getEntity();
        int entityId = event.getEntity().getId();

        try {
            Object level = getLevel(titan);
            if (level == null || isClientSide(level)) return;

            // Check if an inherited ability was just triggered this tick
            // We detect this by watching attackNumber + isAttacking + attackEffectTimer == 0
            if (isAttacking(titan)) {
                int attackNum = getAttackNumber(titan);
                int effectTimer = getAttackEffectTimer(titan);
                boolean effectTriggered = isAttackEffectTriggered(titan);

                // Detect fresh ability trigger: attackNumber 3, effectTimer just reset to 0
                // We use a side-channel: check if the rider triggered slots 5/8/9
                // by reading a flag we set via the ability handler below
                Integer pending = PENDING_ABILITY.get(entityId);

                if (pending != null && !effectTriggered && effectTimer >= 14) {
                    // Fire the ability at effect tick 14 (groundsmash effect tick)
                    Boolean alreadyFired = EFFECT_FIRED.getOrDefault(entityId, false);
                    if (!alreadyFired) {
                        EFFECT_FIRED.put(entityId, true);
                        fireWarhammerAbility(pending, titan, level);
                    }
                }

                // Clean up when attack ends
                if (!isAttacking(titan)) {
                    PENDING_ABILITY.remove(entityId);
                    EFFECT_FIRED.remove(entityId);
                }
            } else {
                PENDING_ABILITY.remove(entityId);
                EFFECT_FIRED.remove(entityId);
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerAbilityEventHandler tick failed: {}",
                    e.getMessage());
        }
    }

    /**
     * Called by the ability trigger packet handler when the player
     * presses ability slots 5, 8, or 9 on the Attack Titan.
     * Queues the ability to fire at the next effect tick.
     */
    public static void queueAbility(int entityId, int abilitySlot) {
        PENDING_ABILITY.put(entityId, abilitySlot);
        EFFECT_FIRED.put(entityId, false);
        AotAddon.LOGGER.debug("[AotAddon] Queued Warhammer ability {} for entity {}",
                abilitySlot, entityId);
    }

    // =========================================================================
    // ABILITY IMPLEMENTATIONS
    // =========================================================================

    private static void fireWarhammerAbility(int abilityId, Object titan, Object level)
            throws Exception {
        double x = getX(titan);
        double y = getY(titan);
        double z = getZ(titan);
        UUID shifterUUID = getShifterUUID(titan);

        switch (abilityId) {
            case 1 -> performStomp(titan, level, x, y, z, shifterUUID);
            case 2 -> performSpikeField(titan, level, x, y, z, shifterUUID);
            case 3 -> performPiercingThorns(titan, level, x, y, z, shifterUUID);
        }
    }

    private static void performStomp(Object titan, Object level,
                                      double x, double y, double z, UUID shifterUUID)
            throws Exception {
        float yawRad = getYawRad(titan);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double centerX = x + forwardX * 4.0;
        double centerZ = z + forwardZ * 4.0;
        int groundY = (int) Math.floor(y);
        double radius = 8.0;

        Object aabb = makeAABB(centerX - radius, groundY - 1, centerZ - radius,
                centerX + radius, groundY + 6, centerZ + radius);
        java.util.List<?> targets = getEntitiesInAABB(level, aabb);

        for (Object target : targets) {
            if (target == titan) continue;
            if (isRiderOf(target, shifterUUID)) continue;
            if (isShifterOrNape(target)) continue;
            double dist = distanceTo(target, centerX, y, centerZ);
            if (dist > radius) continue;
            float damage = (float) (20.0 * (1.0 - dist / radius));
            dealDamage(titan, target, damage);
            applyKnockback(target, centerX, centerZ, 3.0, 1.5);
        }

        drainStamina(shifterUUID, 15.0f);
        AotAddon.LOGGER.debug("[AotAddon] Inherited Stomp fired");
    }

    private static void performSpikeField(Object titan, Object level,
                                           double x, double y, double z, UUID shifterUUID)
            throws Exception {
        int groundY = (int) Math.floor(y);
        int count = 60;
        double maxRadius = 25.0;
        Random rng = new Random();

        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / (double) count + (rng.nextDouble() - 0.5) * 0.6;
            double radius = 4.0 + rng.nextDouble() * maxRadius;
            double sx = x + Math.cos(angle) * radius;
            double sz = z + Math.sin(angle) * radius;
            int surfaceY = findSurfaceY(level, sx, groundY, sz);

            spawnSpike(level, sx, surfaceY, sz,
                    (rng.nextFloat() - 0.5f) * 30f, (rng.nextFloat() - 0.5f) * 30f,
                    9f + rng.nextFloat() * 12f, 1.8f + rng.nextFloat() * 2.4f,
                    (int) ((radius - 4.0) / (maxRadius - 4.0) * 15.0) + rng.nextInt(4),
                    shifterUUID);
        }

        drainStamina(shifterUUID, 15.0f);
        AotAddon.LOGGER.debug("[AotAddon] Inherited Spike Field fired");
    }

    private static void performPiercingThorns(Object titan, Object level,
                                               double x, double y, double z, UUID shifterUUID)
            throws Exception {
        int groundY = (int) Math.floor(y);
        int count = 10;
        Random rng = new Random();

        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / (double) count + (rng.nextDouble() - 0.5) * 0.6;
            double radius = 5.0 + rng.nextDouble() * 20.0;
            double sx = x + Math.cos(angle) * radius;
            double sz = z + Math.sin(angle) * radius;
            int surfaceY = findSurfaceY(level, sx, groundY, sz);

            spawnSpike(level, sx, surfaceY, sz,
                    (rng.nextFloat() - 0.5f) * 30f, (rng.nextFloat() - 0.5f) * 30f,
                    9f + rng.nextFloat() * 12f, 1.8f + rng.nextFloat() * 2.4f,
                    (int) ((radius - 5.0) / 20.0 * 15.0) + rng.nextInt(4),
                    shifterUUID);
        }

        // Direct damage to nearby entities
        double damageRadius = 25.0;
        Object aabb = makeAABB(x - damageRadius, y - 2, z - damageRadius,
                x + damageRadius, y + 6, z + damageRadius);
        java.util.List<?> targets = getEntitiesInAABB(level, aabb);
        for (Object target : targets) {
            if (target == titan) continue;
            if (isRiderOf(target, shifterUUID)) continue;
            if (isShifterOrNape(target)) continue;
            if (distanceTo(target, x, y, z) > damageRadius) continue;
            dealDamage(titan, target, 20.0f);
            applyKnockback(target, x, z, 1.5, 2.0);
        }

        drainStamina(shifterUUID, 45.0f);
        AotAddon.LOGGER.debug("[AotAddon] Inherited Piercing Thorns fired");
    }

    // =========================================================================
    // SHARED HELPERS
    // =========================================================================

    private static void tryLoadClass() {
        classChecked = true;
        try {
            attackTitanClass = Class.forName("daot.AttackTitanEntity");
        } catch (ClassNotFoundException e) {
            AotAddon.LOGGER.warn("[AotAddon] daot.AttackTitanEntity not found");
        }
    }

    private static int findSurfaceY(Object level, double sx, int groundY, double sz)
            throws Exception {
        for (int dy = 10; dy >= -10; dy--) {
            Object pos = makeBlockPos((int) Math.floor(sx), groundY + dy, (int) Math.floor(sz));
            Object above = blockAbove(pos);
            if (isSolid(level, pos) && !isSolid(level, above)) {
                return groundY + dy + 1;
            }
        }
        return groundY;
    }

    private static void spawnSpike(Object level, double x, double y, double z,
                                    float tiltX, float tiltZ, float height, float width,
                                    int delay, UUID ownerUUID) throws Exception {
        Class<?> spikeClass = Class.forName("daot.WarhammerSpikeEntity");
        Class<?> dannysAot = Class.forName("daot.DannysAot");
        Object spikeType = dannysAot.getField("WARHAMMER_SPIKE").get(null);
        Class<?> entityTypeClass = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");

        Object spike = spikeClass.getConstructor(entityTypeClass, levelClass)
                .newInstance(spikeType, level);
        spikeClass.getMethod("method_5814", double.class, double.class, double.class)
                .invoke(spike, x, y, z);
        spikeClass.getMethod("setSpikeProperties", float.class, float.class, float.class, float.class)
                .invoke(spike, tiltX, tiltZ, height, width);
        spikeClass.getMethod("setSpawnDelay", int.class).invoke(spike, delay);
        spikeClass.getMethod("setOwnerUUID", UUID.class).invoke(spike, ownerUUID);
        level.getClass().getMethod("method_8649",
                Class.forName("net.minecraft.world.entity.Entity"))
                .invoke(level, spike);
    }

    private static void dealDamage(Object attacker, Object target, float amount) throws Exception {
        Method getDamageSources = attacker.getClass().getMethod("method_48923");
        Object damageSources = getDamageSources.invoke(attacker);
        Method mobAttack = damageSources.getClass().getMethod("method_48812",
                Class.forName("net.minecraft.world.entity.LivingEntity"));
        Object source = mobAttack.invoke(damageSources, attacker);
        target.getClass().getMethod("method_5643",
                Class.forName("net.minecraft.world.damagesource.DamageSource"), float.class)
                .invoke(target, source, amount);
    }

    private static void applyKnockback(Object target, double fromX, double fromZ,
                                        double horiz, double vert) throws Exception {
        double tx = (double) target.getClass().getMethod("method_23317").invoke(target);
        double tz = (double) target.getClass().getMethod("method_23321").invoke(target);
        double dx = tx - fromX, dz = tz - fromZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) return;
        dx /= len; dz /= len;
        Object vel = target.getClass().getMethod("method_18798").invoke(target);
        Object add = makeVec3(dx * horiz, vert, dz * horiz);
        Object newVel = vel.getClass().getMethod("method_1019",
                Class.forName("net.minecraft.world.phys.Vec3")).invoke(vel, add);
        target.getClass().getMethod("method_18799",
                Class.forName("net.minecraft.world.phys.Vec3")).invoke(target, newVel);
    }

    private static void drainStamina(UUID uuid, float amount) {
        if (uuid == null) return;
        try {
            Class.forName("daot.network.ModNetworking")
                    .getMethod("drainStamina", UUID.class, float.class)
                    .invoke(null, uuid, amount);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] drainStamina failed: {}", e.getMessage());
        }
    }

    private static boolean isRiderOf(Object entity, UUID uuid) {
        if (uuid == null) return false;
        try {
            return ((net.minecraft.world.entity.Entity) entity).getUUID().equals(uuid);
        } catch (Exception e) { return false; }
    }

    private static boolean isShifterOrNape(Object entity) {
        String[] classes = {
            "daot.AttackTitanEntity", "daot.AttackTitanNapeEntity", "daot.AttackTitanEyeEntity",
            "daot.ArmoredTitanEntity", "daot.ArmoredTitanNapeEntity",
            "daot.ColossalTitanEntity", "daot.ColossalTitanNapeEntity",
            "daot.FemaleTitanEntity", "daot.FemaleTitanNapeEntity",
            "daot.BeastTitanEntity", "daot.BeastTitanNapeEntity",
            "daot.WarhammerTitanEntity", "daot.WarhammerTitanNapeEntity"
        };
        for (String c : classes) {
            try { if (Class.forName(c).isInstance(entity)) return true; }
            catch (ClassNotFoundException ignored) {}
        }
        return false;
    }

    private static double distanceTo(Object entity, double x, double y, double z)
            throws Exception {
        double ex = (double) entity.getClass().getMethod("method_23317").invoke(entity);
        double ey = (double) entity.getClass().getMethod("method_23318").invoke(entity);
        double ez = (double) entity.getClass().getMethod("method_23321").invoke(entity);
        double dx = ex-x, dy = ey-y, dz = ez-z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private static java.util.List<?> getEntitiesInAABB(Object level, Object aabb)
            throws Exception {
        return (java.util.List<?>) level.getClass().getMethod("method_18467",
                Class.class,
                Class.forName("net.minecraft.world.phys.AABB"))
                .invoke(level,
                        Class.forName("net.minecraft.world.entity.LivingEntity"), aabb);
    }

    private static Object makeAABB(double x1, double y1, double z1,
                                    double x2, double y2, double z2) throws Exception {
        return Class.forName("net.minecraft.world.phys.AABB")
                .getConstructor(double.class, double.class, double.class,
                        double.class, double.class, double.class)
                .newInstance(x1, y1, z1, x2, y2, z2);
    }

    private static Object makeVec3(double x, double y, double z) throws Exception {
        return Class.forName("net.minecraft.world.phys.Vec3")
                .getConstructor(double.class, double.class, double.class)
                .newInstance(x, y, z);
    }

    private static Object makeBlockPos(int x, int y, int z) throws Exception {
        return Class.forName("net.minecraft.core.BlockPos")
                .getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
    }

    private static Object blockAbove(Object pos) throws Exception {
        return pos.getClass().getMethod("method_10084").invoke(pos);
    }

    private static boolean isSolid(Object level, Object pos) throws Exception {
        Object state = level.getClass().getMethod("method_8320", pos.getClass()).invoke(level, pos);
        return (boolean) state.getClass().getMethod("method_51367").invoke(state);
    }

    private static Object getLevel(Object entity) throws Exception {
        return entity.getClass().getMethod("method_37908").invoke(entity);
    }

    private static boolean isClientSide(Object level) throws Exception {
        return (boolean) level.getClass().getMethod("method_8608").invoke(level);
    }

    private static boolean isAttacking(Object titan) throws Exception {
        return (boolean) titan.getClass().getMethod("isAttacking").invoke(titan);
    }

    private static int getAttackNumber(Object titan) throws Exception {
        return (int) titan.getClass().getMethod("getAttackNumber").invoke(titan);
    }

    private static int getAttackEffectTimer(Object titan) throws Exception {
        Class<?> cls = titan.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField("attackEffectTimer");
                f.setAccessible(true);
                return (int) f.get(titan);
            } catch (NoSuchFieldException ignored) { cls = cls.getSuperclass(); }
        }
        return -1;
    }

    private static boolean isAttackEffectTriggered(Object titan) throws Exception {
        Class<?> cls = titan.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField("attackEffectTriggered");
                f.setAccessible(true);
                return (boolean) f.get(titan);
            } catch (NoSuchFieldException ignored) { cls = cls.getSuperclass(); }
        }
        return false;
    }

    private static float getYawRad(Object entity) throws Exception {
        float yaw = (float) entity.getClass().getMethod("method_36454").invoke(entity);
        return (float) Math.toRadians(yaw);
    }

    private static double getX(Object entity) throws Exception {
        return (double) entity.getClass().getMethod("method_23317").invoke(entity);
    }

    private static double getY(Object entity) throws Exception {
        return (double) entity.getClass().getMethod("method_23318").invoke(entity);
    }

    private static double getZ(Object entity) throws Exception {
        return (double) entity.getClass().getMethod("method_23321").invoke(entity);
    }

    private static UUID getShifterUUID(Object titan) throws Exception {
        Object result = titan.getClass().getMethod("getShifterUUID").invoke(titan);
        if (result instanceof Optional<?> opt) return opt.isPresent() ? (UUID) opt.get() : null;
        return result instanceof UUID u ? u : null;
    }
}
