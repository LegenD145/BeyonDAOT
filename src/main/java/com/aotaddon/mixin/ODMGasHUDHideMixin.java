package com.aotaddon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.aotaddon.AotAddon;

/**
 * Cancels daot's built-in gas bar + low-gas warning rendering (the always-on HUD
 * we're replacing with the tap-to-check action-bar text).
 *
 * Targets ODMGasHUD's private renderGasBar/renderWarning methods directly rather than
 * the HudRenderCallback registration itself, since that's a lambda and not a stable
 * mixin target across builds. These are daot's own custom method names (not overriding
 * a vanilla method), so they keep their literal names, matching the pattern used by
 * ODMGasSuppressionMixin elsewhere in this project.
 */
@Mixin(targets = "daot.ODMGasHUD", remap = false)
public class ODMGasHUDHideMixin {

    @Inject(method = "renderGasBar", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aotaddon$cancelGasBar(CallbackInfo ci) {
        AotAddon.LOGGER.info("[aotaddon] ODMGasHUDHideMixin: renderGasBar injection HIT, cancelling");
        ci.cancel();
    }

    @Inject(method = "renderWarning", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aotaddon$cancelWarning(CallbackInfo ci) {
        AotAddon.LOGGER.info("[aotaddon] ODMGasHUDHideMixin: renderWarning injection HIT, cancelling");
        ci.cancel();
    }
}