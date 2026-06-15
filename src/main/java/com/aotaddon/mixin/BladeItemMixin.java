package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.util.SpearHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixins into BladeItem to replace the boolean ThunderSpear flag
 * with an integer ThunderSpearCount stored in custom data NBT.
 *
 * We target by class name string so that if Danny's mod recompiles
 * and shifts class layout, we don't hard crash — the mixin simply
 * won't apply and logs a warning.
 */
@Mixin(targets = "daot.BladeItem", remap = false)
public class BladeItemMixin {

    /**
     * Intercepts hasThunderSpear(ItemStack) and redirects it to
     * check our integer count instead of the vanilla boolean.
     * Returns true if count > 0.
     */
    @Inject(
        method = "hasThunderSpear(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void onHasThunderSpear(ItemStack stack,
                                           CallbackInfoReturnable<Boolean> cir) {
        try {
            int count = SpearHelper.getCount(stack);
            cir.setReturnValue(count > 0);
            cir.cancel();
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] BladeItemMixin.onHasThunderSpear failed: {}", e.getMessage());
            // Don't cancel — let original method run as fallback
        }
    }

    /**
     * Intercepts setThunderSpear(ItemStack, boolean) so that:
     * - setThunderSpear(stack, true)  sets count to 1 if currently 0
     * - setThunderSpear(stack, false) sets count to 0
     *
     * This keeps any vanilla internal calls working correctly
     * while our load/fire mixins handle the actual stacking logic.
     */
    @Inject(
        method = "setThunderSpear(Lnet/minecraft/world/item/ItemStack;Z)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void onSetThunderSpear(ItemStack stack, boolean loaded,
                                           CallbackInfo ci) {
        try {
            if (loaded) {
                // Only set to 1 if currently empty — don't overwrite stacked count
                int current = SpearHelper.getCount(stack);
                if (current == 0) {
                    SpearHelper.setCount(stack, 1);
                }
            } else {
                // Full clear
                SpearHelper.setCount(stack, 0);
            }
            ci.cancel();
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] BladeItemMixin.onSetThunderSpear failed: {}", e.getMessage());
            // Don't cancel — let original method run as fallback
        }
    }
}
