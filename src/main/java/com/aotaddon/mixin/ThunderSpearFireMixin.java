package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.util.SpearHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Mixin(targets = "daot.network.ModNetworking", remap = false)
public class ThunderSpearFireMixin {

    // Thread local flag to prevent our mixin re-triggering itself
    // during the reflected call. This is allowed — it's a local
    // primitive per-call, not a static cached reflection value.
    private static final ThreadLocal<Boolean> FIRING = ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "handleThunderSpearFire(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/InteractionHand;FF)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onHandleThunderSpearFire(ServerPlayer player,
                                                 InteractionHand hand,
                                                 float clientXRot,
                                                 float clientYRot,
                                                 CallbackInfo ci) {
        // If we're already inside our own fire call, let the
        // original proceed normally — don't intercept again
        if (FIRING.get()) return;

        try {
            ci.cancel();

            // Get the stack from the correct hand
            ItemStack stack;
            try {
                stack = hand == InteractionHand.MAIN_HAND
                        ? player.getMainHandItem()
                        : player.getOffhandItem();
            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Stack fetch in fire failed: {}", e.getMessage());
                return;
            }

            if (!SpearHelper.isBlade(stack)) return;

            int currentCount;
            try {
                currentCount = SpearHelper.getCount(stack);
            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Count read in fire failed: {}", e.getMessage());
                return;
            }

            if (currentCount <= 0) return;

            // Decrement count by 1 first
            try {
                SpearHelper.setCount(stack, currentCount - 1);
            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Count decrement failed: {}", e.getMessage());
                return;
            }

            // Now invoke the original fire method, guarded by our flag
            // so this mixin won't intercept it again
            try {
                FIRING.set(true);

                Class<?> modNetworkingClass = Class.forName("daot.network.ModNetworking");
                Method fireMethod = modNetworkingClass.getDeclaredMethod(
                        "handleThunderSpearFire",
                        ServerPlayer.class,
                        InteractionHand.class,
                        float.class,
                        float.class
                );
                fireMethod.setAccessible(true);

                // Temporarily set count to 1 so the original's
                // hasThunderSpear() check passes
                SpearHelper.setCount(stack, 1);

                fireMethod.invoke(null, player, hand, clientXRot, clientYRot);

                // Original called setThunderSpear(false) which our
                // BladeItemMixin shim set to count 0 — now restore
                // the remaining count
                SpearHelper.setCount(stack, currentCount - 1);

            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Fire reflection failed: {}", e.getMessage());
                // Restore count since fire failed
                try {
                    SpearHelper.setCount(stack, currentCount);
                } catch (Exception ignored) {}
            } finally {
                FIRING.set(false);
            }

            AotAddon.LOGGER.debug("[AotAddon] Fired spear for {}, {} remaining",
                    player.getName().getString(), currentCount - 1);

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] ThunderSpearFireMixin outer catch: {}", e.getMessage());
        }
    }
}