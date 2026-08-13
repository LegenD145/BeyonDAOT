package com.aotaddon.mixin;

import com.aotaddon.util.BastionStateHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Cancels ability triggers on any TitanEntity whose shifter player
 * is currently in bastion state.
 *
 * Targets the base TitanEntity class so it covers all shifters:
 * Attack, Armored, Colossal, Female, Beast, Warhammer, Jaw, etc.
 */
@Mixin(targets = "daot.TitanEntity", remap = false)
public class BastionAbilityMixin {

    @Inject(
            method = "triggerAbility",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onTriggerAbility(int abilityNumber, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object) this;
        if (self.level().isClientSide()) return;

        // Get the shifter player riding this titan
        Entity passenger = self.getFirstPassenger();
        if (passenger == null) return;

        UUID passengerUUID = passenger.getUUID();
        if (BastionStateHandler.isInBastion(passengerUUID)) {
            ci.cancel();
        }
    }
}
