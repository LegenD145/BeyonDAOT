package com.aotaddon.mixin;

import com.aotaddon.util.ZeroHourExplosionHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zero Hour Formula: on Female titan transform, if the player has the
 * "has_zero_hour" tag, trigger a scaled-down (50%) version of the Colossal
 * detonation - crater, knockback (excluding the shifter themselves), and the
 * same three vanilla particle layers Colossal uses.
 *
 * Injects at TAIL of onPlayerShift so the transform has fully completed (titan
 * entity positioned, attributes applied) before we start the explosion sequence.
 *
 * Dual mojmap/intermediary variants since the parameter type (Player) is a
 * vanilla class subject to remapping under Sinytra Connector - see
 * WarhammerAbilityMixin for the same pattern. require = 0 on both so whichever
 * one doesn't match at runtime just silently no-ops instead of crashing mixin
 * application.
 */
@Mixin(targets = "daot.FemaleTitanEntity", remap = false)
public class ZeroHourFormulaMixin {

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
        if (player == null || !player.getTags().contains("has_zero_hour")) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        ZeroHourExplosionHandler.tryStart(self, player.getUUID());
    }
}
