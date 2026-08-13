package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.client.ODMWallClimbHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects at the TAIL of applyHookMovement to:
 * 1. Override the velocity Danny just set with our wall-climb velocity
 * 2. Skip gas consumption while perching
 *
 * We inject at RETURN (tail) so we run AFTER Danny sets velocity — meaning
 * our value wins instead of being overwritten.
 *
 * The method signature in Mojmap (post-Connector remapping):
 *   applyHookMovement(LocalPlayer, HookPoint, HookPoint)
 * But since HookPoint is daot.HookPoint (not a MC class), we use Object in the descriptor.
 */
@Mixin(targets = "daot.ODMTickHandler", remap = false)
public class ODMGasSuppressionMixin {

    /**
     * Runs at the end of applyHookMovement every tick.
     * If we're in wall-climb state, override Danny's velocity with ours.
     */
    @Inject(
            method = "applyHookMovement",
            at = @At("RETURN"),
            remap = false
    )
    private static void onApplyHookMovementReturn(CallbackInfo ci) {
        if (!ODMWallClimbHandler.isCurrentlyPerching()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Apply the climb velocity that ODMWallClimbHandler calculated this tick
        Vec3 climbVel = ODMWallClimbHandler.getPendingVelocity();
        if (climbVel != null) {
            player.setDeltaMovement(climbVel);
            ODMWallClimbHandler.clearPendingVelocity();
        }
    }

    /**
     * Cancels gas consumption while perching by injecting before the consume call.
     * Full Mojmap descriptor since the jar is pre-remapped by Sinytra Connector.
     */
    @Inject(
            method = "applyHookMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Ldaot/DannysAot;consumeGasFromGear(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/player/Player;)V",
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private static void suppressGasWhilePerching(CallbackInfo ci) {
        if (ODMWallClimbHandler.isCurrentlyPerching()) {
            ci.cancel();
        }
    }
}
