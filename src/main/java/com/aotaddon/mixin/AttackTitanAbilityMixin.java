package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.util.WarhammerInheritanceTracker;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Injects into AttackTitanEntity.triggerAbility(int) to add three
 * new ability slots powered by inherited Warhammer Titan abilities:
 *
 *   Slot 5  → Ground Stomp      (Warhammer ability 2 = attackNumber 12)
 *   Slot 8  → Spike Field       (Warhammer ability 4 = attackNumber 8)
 *   Slot 9  → Piercing Thorns   (Warhammer ability 9 no-hammer = attackNumber 11)
 *
 * All three reuse the Attack Titan's existing groundsmash animation
 * (attackNumber 3) for the visual, but execute Warhammer damage logic.
 *
 * Only activates if the rider has Warhammer inheritance stored in their
 * persistent NBT data.
 */
@Mixin(targets = "daot.AttackTitanEntity", remap = false)
public class AttackTitanAbilityMixin {

    @Inject(
        method = "triggerAbility(I)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onTriggerAbility(int abilityNumber, CallbackInfo ci) {
        // Only intercept our new slots — 5, 8, 9
        if (abilityNumber != 5 && abilityNumber != 8 && abilityNumber != 9) return;

        try {
            // Skip if defeated or transforming
            if (isDefeated() || isTransforming() || isDismounting()) return;

            // Get the rider
            UUID shifterUUID = getShifterUUID();
            if (shifterUUID == null) return;

            Object level = getLevel();
            if (level == null || isClientSide(level)) return;

            ServerPlayer rider = getRiderByUUID(level, shifterUUID);
            if (rider == null) return;

            // Check inheritance
            if (!WarhammerInheritanceTracker.hasInheritance(rider)) {
                // No inheritance — cancel silently so nothing happens
                ci.cancel();
                return;
            }

            // Cancel the original (which does nothing for these slots anyway)
            ci.cancel();

            // --- Dispatch to the correct Warhammer ability ---
            switch (abilityNumber) {
                case 5 -> triggerStomp();
                case 8 -> triggerSpikeField();
                case 9 -> triggerPiercingThorns();
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] AttackTitanAbilityMixin.onTriggerAbility failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // ABILITY IMPLEMENTATIONS
    // Each one:
    //   1. Sets attackNumber = 3 (groundsmash anim) on the Attack Titan
    //   2. Calls the corresponding private method on a temporary WarhammerTitanEntity
    //      via reflection to reuse the exact same damage/spike logic
    // =========================================================================

    /**
     * Slot 5 — Ground Stomp.
     * Reuses Warhammer's performGroundSlam() logic (non-charge2 variant).
     * Visual: Attack Titan groundsmash animation (attackNumber 3), 35 ticks.
     */
    private void triggerStomp() throws Exception {
        if (getAttackCooldown() > 0) return;

        setWasMovingOnAttackStart(isMoving());
        setAttackNumber(3);       // groundsmash anim
        setAttacking(true);
        setAttackAnimationTicks(35);
        setAttackCooldown(35);
        resetAttackEffect();

        // Schedule the stomp damage at effect tick 14 (same as groundsmash)
        // We store a flag so tickAttackEffect knows to call our stomp
        setPendingWarhammerAbility(1);

        AotAddon.LOGGER.debug("[AotAddon] Attack Titan triggered inherited Stomp");
    }

    /**
     * Slot 8 — Spike Field.
     * Reuses Warhammer's spike field logic (attackNumber 8 on Warhammer).
     * Visual: Attack Titan groundsmash animation (attackNumber 3), 35 ticks.
     */
    private void triggerSpikeField() throws Exception {
        if (getAttackCooldown() > 0) return;

        setWasMovingOnAttackStart(isMoving());
        setAttackNumber(3);
        setAttacking(true);
        setAttackAnimationTicks(35);
        setAttackCooldown(35);
        resetAttackEffect();

        setPendingWarhammerAbility(2);

        AotAddon.LOGGER.debug("[AotAddon] Attack Titan triggered inherited Spike Field");
    }

    /**
     * Slot 9 — Piercing Thorns.
     * Reuses Warhammer's piercing thorns logic (attackNumber 11 on Warhammer).
     * Visual: Attack Titan groundsmash animation (attackNumber 3), 35 ticks.
     * (Warhammer uses 100 ticks but we shorten to 35 to match AT feel.)
     */
    private void triggerPiercingThorns() throws Exception {
        if (getAttackCooldown() > 0) return;

        setWasMovingOnAttackStart(isMoving());
        setAttackNumber(3);
        setAttacking(true);
        setAttackAnimationTicks(35);
        setAttackCooldown(35);
        resetAttackEffect();

        setPendingWarhammerAbility(3);

        AotAddon.LOGGER.debug("[AotAddon] Attack Titan triggered inherited Piercing Thorns");
    }

    // =========================================================================
    // PENDING ABILITY STORAGE
    // We store which Warhammer ability is pending in a thread-local so the
    // effect tick injection (below) knows what to execute at tick 14.
    // =========================================================================

    private static final ThreadLocal<Integer> PENDING_WH_ABILITY = ThreadLocal.withInitial(() -> 0);

    private void setPendingWarhammerAbility(int id) {
        PENDING_WH_ABILITY.set(id);
    }

    /**
     * Hooks into the attack effect timer tick to fire the actual
     * Warhammer damage at the correct moment (tick 14 = groundsmash effect tick).
     */
    @Inject(
        method = "method_5773()V",
        at = @At(
            value = "INVOKE",
            target = "daot.AttackTitanEntity.dealAttackDamage()V",
            ordinal = 0
        ),
        remap = false,
        cancellable = false
    )
    private void onDealAttackDamage(CallbackInfo ci) {
        int pending = PENDING_WH_ABILITY.get();
        if (pending == 0) return;

        // Only fire for attackNumber 3 (our groundsmash reuse)
        try {
            int attackNum = getAttackNumber();
            if (attackNum != 3) {
                PENDING_WH_ABILITY.set(0);
                return;
            }

            Object level = getLevel();
            if (level == null || isClientSide(level)) {
                PENDING_WH_ABILITY.set(0);
                return;
            }

            // Spawn a temporary WarhammerTitanEntity at our position
            // to call its private damage methods via reflection
            fireWarhammerAbility(pending, level);

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] onDealAttackDamage Warhammer dispatch failed: {}", e.getMessage());
        } finally {
            PENDING_WH_ABILITY.set(0);
        }
    }

    /**
     * Fires the actual Warhammer ability by reflecting into WarhammerTitanEntity
     * and calling its private damage method directly on this entity's position.
     *
     * We do NOT spawn a real Warhammer entity — we just call the static-like
     * private methods by temporarily setting up a proxy call.
     */
    private void fireWarhammerAbility(int abilityId, Object level) throws Exception {
        Class<?> whClass = Class.forName("daot.WarhammerTitanEntity");

        // Get our position to pass context
        Method getXMethod = this.getClass().getMethod("method_23317");
        Method getYMethod = this.getClass().getMethod("method_23318");
        Method getZMethod = this.getClass().getMethod("method_23321");
        double x = (double) getXMethod.invoke(this);
        double y = (double) getYMethod.invoke(this);
        double z = (double) getZMethod.invoke(this);

        UUID shifterUUID = getShifterUUID();

        switch (abilityId) {
            case 1 -> {
                // Stomp — call performGroundSlam via reflection on this entity
                // Since AttackTitanEntity doesn't have this method, we invoke
                // the Warhammer version by temporarily wrapping our context.
                // Simplest safe approach: call the spike/stomp logic directly.
                performInheritedStomp(level, x, y, z, shifterUUID);
            }
            case 2 -> {
                performInheritedSpikeField(level, x, y, z, shifterUUID);
            }
            case 3 -> {
                performInheritedPiercingThorns(level, x, y, z, shifterUUID);
            }
        }
    }

    /**
     * Inherited Stomp — ground shockwave in front of the Attack Titan.
     * Mirrors Warhammer's performGroundSlam (non-charge2 variant).
     */
    private void performInheritedStomp(Object level, double x, double y, double z, UUID shifterUUID) throws Exception {
        if (!isServerLevel(level)) return;

        float yawRad = getYawRad();
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double centerX = x + forwardX * 4.0;
        double centerZ = z + forwardZ * 4.0;
        int groundY = (int) Math.floor(y);

        // Damage entities in stomp radius
        Class<?> livingClass = Class.forName("net.minecraft.world.entity.LivingEntity");
        Method getEntitiesMethod = level.getClass().getMethod("method_18467",
                Class.class,
                Class.forName("net.minecraft.world.phys.AABB"));

        double radius = 8.0;
        Object aabb = makeAABB(centerX - radius, groundY - 1, centerZ - radius,
                centerX + radius, groundY + 6, centerZ + radius);

        java.util.List<?> targets = (java.util.List<?>) getEntitiesMethod.invoke(level, livingClass, aabb);

        for (Object target : targets) {
            if (target == this) continue;
            if (isRiderOf(target, shifterUUID)) continue;
            if (isShifterTitanOrNape(target)) continue;

            double dist = getDistanceTo(target, centerX, y, centerZ);
            if (dist > radius) continue;

            float damage = (float) (20.0 * (1.0 - dist / radius));
            dealDamageTo(target, damage);

            // Knockback
            applyKnockbackTo(target, centerX, centerZ, 3.0, 1.5);
        }

        // Play stomp sound
        playSound(level, x, y, z, "daot.ModSounds", "TITAN_STOMP", 4.0f, 0.8f);

        // Drain stamina
        drainRiderStamina(level, shifterUUID, 15.0f);

        AotAddon.LOGGER.debug("[AotAddon] Inherited Stomp fired at ({}, {}, {})", centerX, y, centerZ);
    }

    /**
     * Inherited Spike Field — spawns WarhammerSpikeEntity instances around the titan.
     * Mirrors Warhammer's spike field ability (attackNumber 8).
     */
    private void performInheritedSpikeField(Object level, double x, double y, double z, UUID shifterUUID) throws Exception {
        if (!isServerLevel(level)) return;

        Class<?> spikeClass = Class.forName("daot.WarhammerSpikeEntity");
        Class<?> dannysAotClass = Class.forName("daot.DannysAot");
        java.lang.reflect.Field spikeTypeField = dannysAotClass.getField("WARHAMMER_SPIKE");
        Object spikeType = spikeTypeField.get(null);

        int groundY = (int) Math.floor(y);
        int spikeCount = 60; // slightly fewer than Warhammer's 90
        double spikeRadius = 25.0;
        java.util.Random rng = new java.util.Random();

        for (int i = 0; i < spikeCount; i++) {
            double angle = Math.PI * 2 * i / (double) spikeCount + (rng.nextDouble() - 0.5) * 0.6;
            double radius = 4.0 + rng.nextDouble() * spikeRadius;
            double spikeX = x + Math.cos(angle) * radius;
            double spikeZ = z + Math.sin(angle) * radius;

            // Find surface Y
            int surfaceY = groundY;
            for (int dy = 10; dy >= -10; dy--) {
                Object checkPos = makeBlockPos((int) Math.floor(spikeX), groundY + dy, (int) Math.floor(spikeZ));
                if (isSolidBlock(level, checkPos) && !isSolidBlock(level, blockPosAbove(checkPos))) {
                    surfaceY = groundY + dy + 1;
                    break;
                }
            }

            float tiltX = (rng.nextFloat() - 0.5f) * 30.0f;
            float tiltZ = (rng.nextFloat() - 0.5f) * 30.0f;
            float height = 9.0f + rng.nextFloat() * 12.0f;
            float width = 1.8f + rng.nextFloat() * 2.4f;
            int spawnDelay = (int) ((radius - 4.0) / (spikeRadius - 4.0) * 15.0) + rng.nextInt(4);

            // Spawn spike entity
            Object spike = spawnSpikeEntity(level, spikeType, spikeX, surfaceY, spikeZ,
                    tiltX, tiltZ, height, width, spawnDelay, shifterUUID);
        }

        drainRiderStamina(level, shifterUUID, 15.0f);
        AotAddon.LOGGER.debug("[AotAddon] Inherited Spike Field fired, {} spikes", spikeCount);
    }

    /**
     * Inherited Piercing Thorns — radial spike burst around the titan.
     * Mirrors Warhammer's performPiercingThorns (attackNumber 11).
     */
    private void performInheritedPiercingThorns(Object level, double x, double y, double z, UUID shifterUUID) throws Exception {
        if (!isServerLevel(level)) return;

        Class<?> spikeClass = Class.forName("daot.WarhammerSpikeEntity");
        Class<?> dannysAotClass = Class.forName("daot.DannysAot");
        java.lang.reflect.Field spikeTypeField = dannysAotClass.getField("WARHAMMER_SPIKE");
        Object spikeType = spikeTypeField.get(null);

        int groundY = (int) Math.floor(y);
        int thornsCount = 10;
        java.util.Random rng = new java.util.Random();

        for (int i = 0; i < thornsCount; i++) {
            double angle = Math.PI * 2 * i / (double) thornsCount + (rng.nextDouble() - 0.5) * 0.6;
            double radius = 5.0 + rng.nextDouble() * 20.0;
            double spikeX = x + Math.cos(angle) * radius;
            double spikeZ = z + Math.sin(angle) * radius;

            int surfaceY = groundY;
            for (int dy = 10; dy >= -10; dy--) {
                Object checkPos = makeBlockPos((int) Math.floor(spikeX), groundY + dy, (int) Math.floor(spikeZ));
                if (isSolidBlock(level, checkPos) && !isSolidBlock(level, blockPosAbove(checkPos))) {
                    surfaceY = groundY + dy + 1;
                    break;
                }
            }

            float tiltX = (rng.nextFloat() - 0.5f) * 30.0f;
            float tiltZ = (rng.nextFloat() - 0.5f) * 30.0f;
            float height = 9.0f + rng.nextFloat() * 12.0f;
            float width = 1.8f + rng.nextFloat() * 2.4f;
            int spawnDelay = (int) ((radius - 5.0) / 20.0 * 15.0) + rng.nextInt(4);

            spawnSpikeEntity(level, spikeType, spikeX, surfaceY, spikeZ,
                    tiltX, tiltZ, height, width, spawnDelay, shifterUUID);
        }

        // Also deal direct damage to nearby entities
        Class<?> livingClass = Class.forName("net.minecraft.world.entity.LivingEntity");
        Method getEntitiesMethod = level.getClass().getMethod("method_18467",
                Class.class,
                Class.forName("net.minecraft.world.phys.AABB"));
        double damageRadius = 25.0;
        Object aabb = makeAABB(x - damageRadius, y - 2, z - damageRadius,
                x + damageRadius, y + 6, z + damageRadius);
        java.util.List<?> targets = (java.util.List<?>) getEntitiesMethod.invoke(level, livingClass, aabb);

        for (Object target : targets) {
            if (target == this) continue;
            if (isRiderOf(target, shifterUUID)) continue;
            if (isShifterTitanOrNape(target)) continue;
            double dist = getDistanceTo(target, x, y, z);
            if (dist > damageRadius) continue;
            dealDamageTo(target, 20.0f);
            applyKnockbackTo(target, x, z, 1.5, 2.0);
        }

        drainRiderStamina(level, shifterUUID, 45.0f);
        AotAddon.LOGGER.debug("[AotAddon] Inherited Piercing Thorns fired");
    }

    // =========================================================================
    // SHARED REFLECTION UTILITIES
    // =========================================================================

    private boolean isServerLevel(Object level) throws Exception {
        return !isClientSide(level);
    }

    private boolean isClientSide(Object level) throws Exception {
        Method m = level.getClass().getMethod("method_8608");
        return (boolean) m.invoke(level);
    }

    private Object getLevel() throws Exception {
        Method m = this.getClass().getMethod("method_37908");
        return m.invoke(this);
    }

    private float getYawRad() throws Exception {
        Method m = this.getClass().getMethod("method_36454");
        float yawDeg = (float) m.invoke(this);
        return (float) Math.toRadians(yawDeg);
    }

    private boolean isDefeated() throws Exception {
        Method m = this.getClass().getMethod("isDefeated");
        return (boolean) m.invoke(this);
    }

    private boolean isTransforming() throws Exception {
        Method m = this.getClass().getMethod("isTransforming");
        return (boolean) m.invoke(this);
    }

    private boolean isDismounting() throws Exception {
        Method m = this.getClass().getMethod("isDismounting");
        return (boolean) m.invoke(this);
    }

    private boolean isMoving() throws Exception {
        Method m = this.getClass().getMethod("isMoving");
        return (boolean) m.invoke(this);
    }

    private int getAttackNumber() throws Exception {
        Method m = this.getClass().getMethod("getAttackNumber");
        return (int) m.invoke(this);
    }

    private int getAttackCooldown() throws Exception {
        Field f = this.getClass().getDeclaredField("attackCooldown");
        f.setAccessible(true);
        return (int) f.get(this);
    }

    private void setAttackCooldown(int v) throws Exception {
        Field f = this.getClass().getDeclaredField("attackCooldown");
        f.setAccessible(true);
        f.set(this, v);
    }

    private void setAttackAnimationTicks(int v) throws Exception {
        Field f = this.getClass().getDeclaredField("attackAnimationTicks");
        f.setAccessible(true);
        f.set(this, v);
    }

    private void setAttackNumber(int v) throws Exception {
        Method m = this.getClass().getMethod("setAttackNumber", int.class);
        m.invoke(this, v);
    }

    private void setAttacking(boolean v) throws Exception {
        Method m = this.getClass().getMethod("setAttacking", boolean.class);
        m.invoke(this, v);
    }

    private void setWasMovingOnAttackStart(boolean v) throws Exception {
        Method m = this.getClass().getMethod("setWasMovingOnAttackStart", boolean.class);
        m.invoke(this, v);
    }

    private void resetAttackEffect() throws Exception {
        Field timer = this.getClass().getDeclaredField("attackEffectTimer");
        timer.setAccessible(true);
        timer.set(this, 0);
        Field triggered = this.getClass().getDeclaredField("attackEffectTriggered");
        triggered.setAccessible(true);
        triggered.set(this, false);
    }

    private UUID getShifterUUID() throws Exception {
        Method m = this.getClass().getMethod("getShifterUUID");
        Object result = m.invoke(this);
        if (result instanceof Optional<?> opt) {
            return opt.isPresent() ? (UUID) opt.get() : null;
        }
        return result instanceof UUID u ? u : null;
    }

    private ServerPlayer getRiderByUUID(Object level, UUID uuid) throws Exception {
        Method getServerMethod = level.getClass().getMethod("getServer");
        Object server = getServerMethod.invoke(level);
        Method getPlayerListMethod = server.getClass().getMethod("getPlayerList");
        Object playerList = getPlayerListMethod.invoke(server);
        Method getPlayerMethod = playerList.getClass().getMethod("getPlayer", UUID.class);
        Object player = getPlayerMethod.invoke(playerList, uuid);
        return player instanceof ServerPlayer sp ? sp : null;
    }

    private boolean isRiderOf(Object entity, UUID shifterUUID) {
        if (shifterUUID == null) return false;
        try {
            if (!(entity instanceof net.minecraft.world.entity.Entity e)) return false;
            return e.getUUID().equals(shifterUUID);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isShifterTitanOrNape(Object entity) {
        try {
            String[] shifterClasses = {
                "daot.AttackTitanEntity", "daot.AttackTitanNapeEntity", "daot.AttackTitanEyeEntity",
                "daot.ArmoredTitanEntity", "daot.ArmoredTitanNapeEntity", "daot.ArmoredTitanEyeEntity",
                "daot.ColossalTitanEntity", "daot.ColossalTitanNapeEntity", "daot.ColossalTitanEyeEntity",
                "daot.FemaleTitanEntity", "daot.FemaleTitanNapeEntity", "daot.FemaleTitanEyeEntity",
                "daot.BeastTitanEntity", "daot.BeastTitanNapeEntity", "daot.BeastTitanEyeEntity",
                "daot.WarhammerTitanEntity", "daot.WarhammerTitanNapeEntity", "daot.WarhammerTitanEyeEntity"
            };
            for (String className : shifterClasses) {
                try {
                    if (Class.forName(className).isInstance(entity)) return true;
                } catch (ClassNotFoundException ignored) {}
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private double getDistanceTo(Object entity, double x, double y, double z) throws Exception {
        Method getX = entity.getClass().getMethod("method_23317");
        Method getY = entity.getClass().getMethod("method_23318");
        Method getZ = entity.getClass().getMethod("method_23321");
        double ex = (double) getX.invoke(entity);
        double ey = (double) getY.invoke(entity);
        double ez = (double) getZ.invoke(entity);
        double dx = ex - x, dy = ey - y, dz = ez - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void dealDamageTo(Object target, float amount) throws Exception {
        // Use the entity's own damage source via method_48923().method_48812(this)
        Method getDamageSourcesMethod = this.getClass().getMethod("method_48923");
        Object damageSources = getDamageSourcesMethod.invoke(this);
        Method mobAttackMethod = damageSources.getClass().getMethod("method_48812",
                Class.forName("net.minecraft.world.entity.LivingEntity"));
        Object damageSource = mobAttackMethod.invoke(damageSources, this);
        Method hurtMethod = target.getClass().getMethod("method_5643",
                Class.forName("net.minecraft.world.damagesource.DamageSource"), float.class);
        hurtMethod.invoke(target, damageSource, amount);
    }

    private void applyKnockbackTo(Object target, double fromX, double fromZ,
                                   double horizontal, double vertical) throws Exception {
        Method getX = target.getClass().getMethod("method_23317");
        Method getZ = target.getClass().getMethod("method_23321");
        double tx = (double) getX.invoke(target);
        double tz = (double) getZ.invoke(target);
        double dx = tx - fromX, dz = tz - fromZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) return;
        dx /= len; dz /= len;

        Method getVelMethod = target.getClass().getMethod("method_18798");
        Object currentVel = getVelMethod.invoke(target);
        Method addMethod = currentVel.getClass().getMethod("method_1019",
                Class.forName("net.minecraft.world.phys.Vec3"));
        Object addVec = makeVec3(dx * horizontal, vertical, dz * horizontal);
        Object newVel = addMethod.invoke(currentVel, addVec);
        Method setVelMethod = target.getClass().getMethod("method_18799",
                Class.forName("net.minecraft.world.phys.Vec3"));
        setVelMethod.invoke(target, newVel);
    }

    private void drainRiderStamina(Object level, UUID shifterUUID, float amount) {
        try {
            if (shifterUUID == null) return;
            Class<?> modNetworkingClass = Class.forName("daot.network.ModNetworking");
            Method drainMethod = modNetworkingClass.getMethod("drainStamina", UUID.class, float.class);
            drainMethod.invoke(null, shifterUUID, amount);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] drainRiderStamina failed: {}", e.getMessage());
        }
    }

    private void playSound(Object level, double x, double y, double z,
                           String soundsClass, String soundField,
                           float volume, float pitch) {
        try {
            Class<?> modSoundsClass = Class.forName(soundsClass);
            java.lang.reflect.Field sf = modSoundsClass.getField(soundField);
            Object sound = sf.get(null);
            Class<?> soundSourceClass = Class.forName("net.minecraft.sounds.SoundSource");
            Object ambient = java.util.Arrays.stream(soundSourceClass.getEnumConstants())
                    .filter(e -> e.toString().equals("AMBIENT")).findFirst().orElse(null);
            Method playMethod = level.getClass().getMethod("method_43128",
                    Class.forName("net.minecraft.world.entity.Entity"),
                    double.class, double.class, double.class,
                    Class.forName("net.minecraft.sounds.SoundEvent"),
                    soundSourceClass, float.class, float.class);
            playMethod.invoke(level, null, x, y, z, sound, ambient, volume, pitch);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] playSound failed: {}", e.getMessage());
        }
    }

    private Object makeAABB(double x1, double y1, double z1,
                             double x2, double y2, double z2) throws Exception {
        Class<?> aabbClass = Class.forName("net.minecraft.world.phys.AABB");
        return aabbClass.getConstructor(double.class, double.class, double.class,
                double.class, double.class, double.class)
                .newInstance(x1, y1, z1, x2, y2, z2);
    }

    private Object makeVec3(double x, double y, double z) throws Exception {
        Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
        return vec3Class.getConstructor(double.class, double.class, double.class)
                .newInstance(x, y, z);
    }

    private Object makeBlockPos(int x, int y, int z) throws Exception {
        Class<?> bpClass = Class.forName("net.minecraft.core.BlockPos");
        return bpClass.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
    }

    private boolean isSolidBlock(Object level, Object blockPos) throws Exception {
        Method getBlockState = level.getClass().getMethod("method_8320", blockPos.getClass());
        Object blockState = getBlockState.invoke(level, blockPos);
        Method isSolid = blockState.getClass().getMethod("method_51367");
        return (boolean) isSolid.invoke(blockState);
    }

    private Object blockPosAbove(Object blockPos) throws Exception {
        Method above = blockPos.getClass().getMethod("method_10084");
        return above.invoke(blockPos);
    }

    private Object spawnSpikeEntity(Object level, Object spikeType,
                                     double x, double y, double z,
                                     float tiltX, float tiltZ,
                                     float height, float width,
                                     int spawnDelay, UUID ownerUUID) throws Exception {
        Class<?> spikeClass = Class.forName("daot.WarhammerSpikeEntity");
        Class<?> entityTypeClass = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");

        Object spike = spikeClass.getConstructor(entityTypeClass, levelClass)
                .newInstance(spikeType, level);

        Method setPos = spikeClass.getMethod("method_5814", double.class, double.class, double.class);
        setPos.invoke(spike, x, y, z);

        Method setSpikeProps = spikeClass.getMethod("setSpikeProperties",
                float.class, float.class, float.class, float.class);
        setSpikeProps.invoke(spike, tiltX, tiltZ, height, width);

        Method setDelay = spikeClass.getMethod("setSpawnDelay", int.class);
        setDelay.invoke(spike, spawnDelay);

        Method setOwner = spikeClass.getMethod("setOwnerUUID", UUID.class);
        setOwner.invoke(spike, ownerUUID);

        Method addEntity = level.getClass().getMethod("method_8649",
                Class.forName("net.minecraft.world.entity.Entity"));
        addEntity.invoke(level, spike);

        return spike;
    }
}
