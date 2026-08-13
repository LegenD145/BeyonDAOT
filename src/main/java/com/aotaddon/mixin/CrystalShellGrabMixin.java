package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Injects into AttackTitanEntity.tryGrabNearest().
 *
 * When the Attack Titan grabs near a CrystalShellWarhammerEntity:
 * - The grab is cancelled
 * - A WarhammerSpikeEntity is spawned directly beneath the Attack Titan
 * - The Attack Titan is impaled and frozen in place
 * - A 5 minute per-Warhammer-titan cooldown applies
 *
 * We bypass performImpale() entirely and spawn the spike ourselves so we
 * can place it exactly at the Attack Titan's position rather than 10 blocks
 * in front of the Warhammer titan.
 */
@Mixin(targets = "daot.AttackTitanEntity", remap = false)
public class CrystalShellGrabMixin {

    private static final double GRAB_RANGE  = 15.0;
    private static final double GRAB_HEIGHT = 6.0;

    /** 5 minutes in ticks */
    private static final int PASSIVE_COOLDOWN_TICKS = 6000;

    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    @Inject(
            method = "triggerAbility",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onTriggerAbility(int abilityNumber, CallbackInfo ci) {
        if (abilityNumber != 7) return; // Only intercept grab

        LivingEntity self = (LivingEntity)(Object) this;
        if (self.level().isClientSide()) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        AotAddon.LOGGER.info("[CrystalShellGrab] triggerAbility(7) intercepted on Attack Titan");

        // Check if already grabbing — if so let Danny handle release
        try {
            Class<?> attackClass = Class.forName("daot.AttackTitanEntity");
            boolean isGrabbing = (boolean) attackClass.getMethod("isGrabbing").invoke(self);
            if (isGrabbing) return;
        } catch (Exception e) {
            AotAddon.LOGGER.error("[CrystalShellGrab] isGrabbing check failed: {}", e.toString());
            return;
        }

        // Forward-biased search box
        float yawRad = (float) Math.toRadians(self.getYRot());
        double fwdX  = -Math.sin(yawRad);
        double fwdZ  =  Math.cos(yawRad);
        AABB searchBox = self.getBoundingBox()
                .inflate(GRAB_RANGE, GRAB_HEIGHT, GRAB_RANGE)
                .move(fwdX * 4.0, 0.0, fwdZ * 4.0);

        // Find a CrystalShellWarhammerEntity in range
        List<Entity> candidates = self.level().getEntities(self, searchBox, e -> {
            try { return Class.forName("daot.CrystalShellWarhammerEntity").isInstance(e); }
            catch (ClassNotFoundException ex) { return false; }
        });
        if (candidates.isEmpty()) return;

        // Pick closest one roughly in front
        Entity bestShell = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity e : candidates) {
            double dx = e.getX() - self.getX();
            double dz = e.getZ() - self.getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist < 0.001) continue;
            if ((dx * fwdX + dz * fwdZ) / horizDist < 0.4) continue;
            double distSq = dx * dx + dz * dz;
            if (distSq < bestDistSq) { bestDistSq = distSq; bestShell = e; }
        }
        if (bestShell == null) return;

        try {
            Class<?> shellClass     = Class.forName("daot.CrystalShellWarhammerEntity");
            Class<?> warhammerClass = Class.forName("daot.WarhammerTitanEntity");
            Class<?> spikeClass     = Class.forName("daot.WarhammerSpikeEntity");
            Class<?> dannysAotClass = Class.forName("daot.DannysAot");

            // Get the Warhammer titan from the shell
            int parentId = (int) shellClass.getMethod("getParentTitanId").invoke(bestShell);
            if (parentId == -1) return;
            Entity parentEntity = self.level().getEntity(parentId);
            if (parentEntity == null || !warhammerClass.isInstance(parentEntity)) return;

            // Cooldown check
            UUID warhammerUUID = parentEntity.getUUID();
            long now = self.level().getGameTime();
            Long expiresAt = cooldowns.get(warhammerUUID);
            if (expiresAt != null && now < expiresAt) {
                // Passive on cooldown — grab proceeds normally
                return;
            }

            // --- Spawn spike directly beneath the Attack Titan ---
            // spikeX/Z = Attack Titan's position, spikeY = one block below their feet
            double spikeX = self.getX();
            double spikeZ = self.getZ();
            int    spikeY = (int) Math.floor(self.getY()) - 1;

            // Get the WARHAMMER_SPIKE entity type from DannysAot
            Object spikeType = dannysAotClass.getField("WARHAMMER_SPIKE").get(null);

            // Construct WarhammerSpikeEntity(EntityType, Level)
            Object spike = spikeClass
                    .getConstructor(spikeType.getClass().getInterfaces()[0], net.minecraft.world.level.Level.class)
                    .newInstance(spikeType, serverLevel);

            // Position the spike
            ((Entity) spike).moveTo(spikeX, spikeY, spikeZ);

            // setSpikeProperties(tiltX, tiltZ, height, width) — same as performImpale uses
            spikeClass.getMethod("setSpikeProperties", float.class, float.class, float.class, float.class)
                    .invoke(spike, 0.0f, 0.0f, 53.0f, 12.0f);
            spikeClass.getMethod("setSpawnDelay", int.class).invoke(spike, 0);
            spikeClass.getMethod("setImpaleSpike", boolean.class).invoke(spike, true);

            // Set owner to the Warhammer shifter's UUID
            UUID shifterUUID = (UUID) warhammerClass.getMethod("getShifterUUID").invoke(parentEntity);
            if (shifterUUID != null) {
                spikeClass.getMethod("setOwnerUUID", UUID.class).invoke(spike, shifterUUID);
            }

            // Link the spike to the Attack Titan and freeze them
            spikeClass.getMethod("setImpaleTargetId", int.class).invoke(spike, self.getId());
            warhammerClass.getMethod("setImpaled", boolean.class) // Actually on AttackTitanEntity
                    .invoke(null, (Object) null); // We call it on self directly below

            // setImpaled is on AttackTitanEntity itself (self)
            Class<?> attackClass = Class.forName("daot.AttackTitanEntity");
            attackClass.getMethod("setImpaled", boolean.class).invoke(self, true);
            attackClass.getMethod("setDismounting", boolean.class).invoke(self, false);

            // Zero velocity and set noPhysics (field_6037) to freeze them
            self.setDeltaMovement(0, 0, 0);
            self.noPhysics = true;

            // Spawn the spike into the world
            serverLevel.addFreshEntity((Entity) spike);

            // Set cooldown
            cooldowns.put(warhammerUUID, now + PASSIVE_COOLDOWN_TICKS);

            // Cancel the grab
            ci.cancel();

            AotAddon.LOGGER.info("[CrystalShellGrab] Spike spawned at Attack Titan position ({}, {}, {})",
                    spikeX, spikeY, spikeZ);

        } catch (Exception e) {
            AotAddon.LOGGER.error("[CrystalShellGrab] Reflection failed: {}", e.toString());
        }
    }
}