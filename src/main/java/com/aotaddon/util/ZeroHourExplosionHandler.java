package com.aotaddon.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Zero Hour Formula: a scaled-down (50%) replica of Colossal's detonation,
 * triggered on Female titan transform via ZeroHourFormulaMixin. Fully addon-owned
 * state machine - does NOT reflectively drive Colossal's own instance fields,
 * since those live on ColossalTitanEntity's own instance and can't be reused
 * cross-instance against a FemaleTitanEntity.
 *
 * Scale reference (from decompiled ColossalTitanEntity explosion phase code):
 *   Colossal's big particle burst used a 40/60/30-count spread at several block
 *   radius. We run everything here at 50% radius/particle-count/damage per
 *   Bodi's balance call.
 *
 * Uses vanilla level.explode() and vanilla ParticleTypes directly - both are
 * plain Minecraft API, not daot internals, so no reflection is needed for the
 * explosion/visual side of this at all.
 */
public final class ZeroHourExplosionHandler {

    // --- Tunable scale constants (50% of Colossal's approximate values) ---
    private static final double CRATER_RADIUS = 7.0;      // Colossal-scale ~14
    private static final float EXPLOSION_STRENGTH = 4.0f;  // vanilla explosion power
    private static final double KNOCKBACK_RADIUS = 10.0;
    private static final double KNOCKBACK_STRENGTH = 1.5;
    private static final int PARTICLE_LARGE_SMOKE = 20; // Colossal ~40
    private static final int PARTICLE_EXPLOSION = 30;    // Colossal ~60
    private static final int PARTICLE_CLOUD = 15;         // Colossal ~30

    private static final int PHASE_COUNT = 6; // spread the burst over a handful of ticks
    private static final int TICKS_PER_PHASE = 2;

    private static final List<ExplosionJob> activeJobs = new ArrayList<>();

    private ZeroHourExplosionHandler() {}

    public static void tryStart(LivingEntity femaleShifter, UUID shifterPlayerUUID) {
        if (!(femaleShifter.level() instanceof ServerLevel serverLevel)) return;

        ExplosionJob job = new ExplosionJob();
        job.level = serverLevel;
        job.centerX = femaleShifter.getX();
        job.centerY = femaleShifter.getY();
        job.centerZ = femaleShifter.getZ();
        job.shifterPlayerUUID = shifterPlayerUUID;
        job.ticksRemaining = TICKS_PER_PHASE;
        job.phase = 0;
        activeJobs.add(job);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (activeJobs.isEmpty()) return;
        Iterator<ExplosionJob> it = activeJobs.iterator();
        while (it.hasNext()) {
            ExplosionJob job = it.next();
            job.ticksRemaining--;
            if (job.ticksRemaining > 0) continue;
            job.ticksRemaining = TICKS_PER_PHASE;

            runPhase(job);
            job.phase++;
            if (job.phase >= PHASE_COUNT) {
                it.remove();
            }
        }
    }

    private static void runPhase(ExplosionJob job) {
        ServerLevel level = job.level;
        double x = job.centerX, y = job.centerY, z = job.centerZ;

        switch (job.phase) {
            case 0 -> {
                // Initial crack - small warning burst before the main blast
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z,
                        PARTICLE_LARGE_SMOKE / 2, 2.0, 1.0, 2.0, 0.05);
            }
            case 1 -> {
                // Main blast: vanilla explosion (block damage handled by vanilla),
                // no fire (matches a "clean" detonation rather than incendiary).
                // Using the simple explode() overload (entity source, position,
                // power, fire, interaction mode) rather than manually building a
                // DamageSource - fewer moving parts to get wrong against your
                // exact mappings version.
                level.explode(null, x, y + 1, z, EXPLOSION_STRENGTH, false,
                        net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);

                // Layered vanilla particle burst - same three types Colossal uses,
                // counts/spread scaled to 50%.
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z,
                        PARTICLE_LARGE_SMOKE, 2.0, 4.0, 2.0, 0.05);
                level.sendParticles(ParticleTypes.EXPLOSION, x, y, z,
                        PARTICLE_EXPLOSION, 2.5, 5.0, 2.5, 0.05);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z,
                        PARTICLE_CLOUD, 1.5, 3.0, 1.5, 0.05);

                applyKnockbackAndDamage(job);
            }
            case 2, 3 -> {
                // Rolling smoke aftermath
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z,
                        PARTICLE_LARGE_SMOKE / 2, CRATER_RADIUS * 0.4, 1.0, CRATER_RADIUS * 0.4, 0.02);
            }
            default -> {
                // trailing dissipation, cheap - just a few cloud puffs
                level.sendParticles(ParticleTypes.CLOUD, x, y + 1, z,
                        5, CRATER_RADIUS * 0.5, 1.0, CRATER_RADIUS * 0.5, 0.01);
            }
        }
    }

    private static void applyKnockbackAndDamage(ExplosionJob job) {
        ServerLevel level = job.level;
        Vec3 center = new Vec3(job.centerX, job.centerY, job.centerZ);
        AABB area = new AABB(
                job.centerX - KNOCKBACK_RADIUS, job.centerY - KNOCKBACK_RADIUS, job.centerZ - KNOCKBACK_RADIUS,
                job.centerX + KNOCKBACK_RADIUS, job.centerY + KNOCKBACK_RADIUS, job.centerZ + KNOCKBACK_RADIUS
        );

        for (Entity entity : level.getEntities((Entity) null, area)) {
            if (!(entity instanceof LivingEntity living)) continue;

            // Self-exclusion: don't knock back the Female shifter's own player,
            // per Bodi's call - doesn't make sense for the shifter to get pushed
            // by their own transformation.
            if (entity instanceof Player p && p.getUUID().equals(job.shifterPlayerUUID)) continue;

            double dx = entity.getX() - job.centerX;
            double dz = entity.getZ() - job.centerZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > KNOCKBACK_RADIUS) continue;
            if (dist < 0.1) { dx = 0.1; dist = 0.1; }

            double falloff = 1.0 - (dist / KNOCKBACK_RADIUS);
            double push = KNOCKBACK_STRENGTH * falloff;
            living.setDeltaMovement(
                    living.getDeltaMovement().add((dx / dist) * push, 0.3 * falloff, (dz / dist) * push)
            );
            // Using the generic damage source to keep this compiling safely
            // against your exact mappings - swap to
            // level.damageSources().explosion(...) once you've confirmed its
            // exact overload (Explosion vs Entity,Entity varies by version).
            living.hurt(level.damageSources().generic(), 4.0f * (float) falloff);
        }
    }

    private static final class ExplosionJob {
        ServerLevel level;
        double centerX, centerY, centerZ;
        UUID shifterPlayerUUID;
        int phase;
        int ticksRemaining;
    }
}
