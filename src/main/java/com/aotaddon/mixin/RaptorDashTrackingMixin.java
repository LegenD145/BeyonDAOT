package com.aotaddon.mixin;

import com.aotaddon.access.DaotBridge;
import com.aotaddon.util.RaptorDashHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raptor Compound - dash combo detection.
 *
 * Hooks daot.ShifterDodgeManager.tryStart(ServerPlayer, int) at TAIL. tryStart has
 * several early `return;` statements for invalid dodges (on cooldown, insufficient
 * stamina, movement locked, dismounting, etc.) - injecting at TAIL means this only
 * fires when a dodge actually SUCCEEDED, so we get validated dodges for free
 * without re-checking any of daot's own gating logic ourselves.
 *
 * NOTE: this is daot's own titan dodge system (left-alt + direction, applies
 * velocity to the mounted titan) - NOT this addon's separate ODM double-tap-S
 * i-frame dodge (DodgeStartPayload/DodgeIFrameHandler), which only affects
 * on-foot players and is unrelated.
 *
 * Static method target, so no `this`/(LivingEntity) cast needed here (unlike the
 * FemaleTitanEntity mixins) - mirrors WarhammerAbilityMixin's shape for a static
 * daot method.
 */
@Mixin(targets = "daot.ShifterDodgeManager", remap = false)
public class RaptorDashTrackingMixin {

    @Inject(
            method = "tryStart(Lnet/minecraft/server/level/ServerPlayer;I)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private static void onTryStartMojmap(ServerPlayer player, int direction, CallbackInfo ci) {
        handle(player);
    }

    @Inject(
            method = "tryStart(Lnet/minecraft/class_3222;I)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private static void onTryStartIntermediary(ServerPlayer player, int direction, CallbackInfo ci) {
        handle(player);
    }

    private static void handle(ServerPlayer player) {
        if (player == null) return;
        if (!player.getTags().contains("has_raptor")) return;
        if (!player.isSprinting()) return;
        if (!DaotBridge.isRidingFemaleTitan(player)) return;

        RaptorDashHandler.onSuccessfulDash(player);
    }
}
