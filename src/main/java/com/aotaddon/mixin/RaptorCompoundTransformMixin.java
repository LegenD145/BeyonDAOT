package com.aotaddon.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raptor Compound - base speed nerf.
 *
 * Confirmed via decompile: Female's genericMovementSpeed base is 1.4025, actually
 * HIGHER than Attack's 0.85 in this build. Per Bodi's explicit instruction this is
 * a deliberate nerf despite that discrepancy - Female's sprint gets dropped to
 * Attack's 0.85 baseline as the new default the moment Raptor Compound is active.
 *
 * Uses AttributeInstance#setBaseValue() rather than an AttributeModifier, since we
 * want to actually override the declared base to an absolute value (0.85), not
 * apply a relative +/-% modifier on top of whatever daot's base happens to be.
 * No removal/revert logic needed - this runs fresh every time onPlayerShift fires
 * (i.e. every transformation), and the Female titan entity instance itself (and
 * its attribute state) is discarded when the player reverts to human form, so
 * there's nothing to clean up.
 */
@Mixin(targets = "daot.FemaleTitanEntity", remap = false)
public class RaptorCompoundTransformMixin {

    private static final double NERFED_BASE_SPEED = 0.85;

    @Inject(
            method = "onPlayerShift(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void onPlayerShiftMojmap(Player player, CallbackInfo ci) {
        trigger(player);
    }

    @Inject(
            method = "onPlayerShift(Lnet/minecraft/class_1657;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void onPlayerShiftIntermediary(Player player, CallbackInfo ci) {
        trigger(player);
    }

    private void trigger(Player player) {
        if (player == null || !player.getTags().contains("has_raptor")) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        AttributeInstance speedAttribute = self.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(NERFED_BASE_SPEED);
        }
    }
}
