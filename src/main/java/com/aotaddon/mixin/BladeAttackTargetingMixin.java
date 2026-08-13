package com.aotaddon.mixin;

import com.aotaddon.combat.HitboxDeduper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

/**
 * daot.BladeAttackTracker#tick() calls Level#getEntities three times (air
 * swing AoE, look-direction cone, ground bonus cone) and damages every
 * result with no per-titan deduplication. Since TitanNapeEntity and
 * TitanEyeEntity sit close together on the same titan, both routinely land
 * in the same swept volume, causing one swing to damage both at once
 * regardless of aim. This mixin redirects all three call sites through
 * HitboxDeduper so at most one sub-hitbox entity per titan survives the
 * filter; everything else in the sweep (players, mobs) is untouched.
 *
 * Targets daot by string (never a compile-time dependency) - resolved via
 * AotAddonMixinPlugin the same way as our other daot-target mixins.
 */
@Mixin(targets = "daot.BladeAttackTracker")
public class BladeAttackTargetingMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    // Level#getEntities(Entity, AABB, Predicate<? super Entity>) : List<Entity>
                    target = "Lnet/minecraft/class_1937;method_8333(Lnet/minecraft/class_1297;Lnet/minecraft/class_238;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private static List<Entity> aotaddon$dedupeTitanHitboxes(
            Level level, Entity attacker, AABB area, Predicate<Entity> predicate) {
        List<Entity> raw = level.getEntities(attacker, area, predicate);
        return HitboxDeduper.collapseToClosestPerTitan(raw, attacker);
    }
}