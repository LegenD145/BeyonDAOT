package com.aotaddon.mixin;

import com.aotaddon.gear.GearPouchHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects Danny's blade reload logic to read from the player's GearPouch
 * instead of scanning the entire inventory.
 *
 * Target: daot.network.ModNetworking
 *   - countBladeComponents(ServerPlayer) -> int
 *   - consumeBladeComponents(ServerPlayer, int) -> void
 */
@Mixin(targets = "daot.network.ModNetworking", remap = false)
public class BladeReloadMixin {

    /**
     * Replace countBladeComponents entirely — return pouch count instead.
     */
    @Inject(
            method = "countBladeComponents",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void aotaddon_countFromPouch(
            Object player,
            CallbackInfoReturnable<Integer> cir) {

        if (player instanceof Player p) {
            int count = GearPouchHelper.countBladeComponents(p);
            cir.setReturnValue(count);
        }
    }

    /**
     * Replace consumeBladeComponents entirely — consume from pouch instead.
     */
    @Inject(
            method = "consumeBladeComponents",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void aotaddon_consumeFromPouch(
            Object player,
            int amount,
            CallbackInfo ci) {

        if (player instanceof Player p) {
            GearPouchHelper.consumeBladeComponents(p, amount);
            ci.cancel();
        }
    }
}