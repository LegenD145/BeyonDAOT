package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.util.WarhammerAbilityHandler;
import com.aotaddon.util.WarhammerInheritanceTracker;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "daot.network.ModNetworking", remap = false)
public class WarhammerAbilityMixin {

    @Inject(
            method = "handleTitanAbility(Lnet/minecraft/server/level/ServerPlayer;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void onHandleTitanAbilityMojmap(ServerPlayer player, int abilityNumber, CallbackInfo ci) {
        handleIt(player, abilityNumber, ci);
    }

    @Inject(
            method = "handleTitanAbility(Lnet/minecraft/class_3222;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void onHandleTitanAbilityIntermediary(ServerPlayer player, int abilityNumber, CallbackInfo ci) {
        handleIt(player, abilityNumber, ci);
    }

    private static void handleIt(ServerPlayer player, int abilityNumber, CallbackInfo ci) {
        if (abilityNumber != 5 && abilityNumber != 8 && abilityNumber != 9) return;

        try {
            Object vehicle = player.getVehicle();
            if (vehicle == null) return;

            Class<?> attackTitanClass = Class.forName("daot.AttackTitanEntity");
            if (!attackTitanClass.isInstance(vehicle)) return;

            java.lang.reflect.Method getShifterUUID = attackTitanClass.getMethod("getShifterUUID");
            java.util.UUID shifterUUID = (java.util.UUID) getShifterUUID.invoke(vehicle);
            if (shifterUUID == null || !shifterUUID.equals(player.getUUID())) return;

            if (!WarhammerInheritanceTracker.hasInheritance(player)) return;

            AotAddon.LOGGER.info("[AotAddon] WarhammerAbilityMixin intercepted slot {}", abilityNumber);
            ci.cancel();
            WarhammerAbilityHandler.handleAbility(player, vehicle, abilityNumber);

        } catch (ClassNotFoundException e) {
            // daot not loaded
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] WarhammerAbilityMixin error: {}", e.getMessage());
        }
    }
}