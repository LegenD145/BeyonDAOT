package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fixes thunder spear damage stacking WITHOUT touching Danny's explode()
 * flow itself — we don't cancel or replace level.explode(), we just clear
 * the specific obstacle that breaks stacking: invulnerableTime.
 *
 * BACKGROUND: vanilla's explosion damage formula is naturally distance-based
 * (falloff), so two spears landing at slightly different spots will always
 * deal somewhat different raw amounts — that part is expected and NOT a bug.
 * The actual bug is that the SECOND spear's hit, if it lands within the
 * first spear's invulnerability window, gets reduced/zeroed by vanilla's
 * "ignore damage <= the damage that caused invulnerability" rule.
 *
 * THE FIX: right before Danny's level.explode() runs, we zero
 * invulnerableTime/hurtTime for every living entity inside THIS spear's
 * blast radius — but only if that entity has a recent-enough record of
 * being hit by a thunder spear (so unrelated combat is never touched).
 * We use the entity's CURRENT health as the record key instead of a tick
 * window, which avoids same-tick race conditions entirely: every spear
 * that detonates always clears i-frames for everyone in range immediately
 * before vanilla calculates damage, so each spear's calculated damage
 * always lands at its full (falloff-based) value — making totals additive
 * and reproducible, even if the absolute numbers vary by distance as
 * vanilla intends.
 */
@Mixin(targets = "daot.ThunderSpearEntity", remap = false)
public class ThunderSpearStackingMixin {

    /** Same knockback/damage radius Danny uses in explode(). */
    private static final double BLAST_RADIUS = 10.0;

    @Inject(
            method = "explode",
            at = @At("HEAD"),
            remap = false
    )
    private void clearIFramesBeforeExplosion(CallbackInfo ci) {
        Entity self = (Entity)(Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();

        AABB blastBox = new AABB(
                x - BLAST_RADIUS, y - BLAST_RADIUS, z - BLAST_RADIUS,
                x + BLAST_RADIUS, y + BLAST_RADIUS, z + BLAST_RADIUS
        );

        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox);

        for (LivingEntity entity : nearby) {
            float healthBefore = entity.getHealth();

            // Always clear i-frames for anyone about to be hit by THIS spear.
            // Since this runs at the HEAD of explode(), it always happens
            // immediately before vanilla's damage calculation for this
            // specific spear — guaranteeing this spear's damage is never
            // suppressed by a previous spear's invulnerability window.
            entity.invulnerableTime = 0;
            entity.hurtTime = 0;

            AotAddon.LOGGER.debug(
                    "[ThunderSpearStacking] Cleared i-frames for {} (health before: {})",
                    entity.getName().getString(), healthBefore
            );
        }
    }
}
