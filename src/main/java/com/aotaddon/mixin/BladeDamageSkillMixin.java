package com.aotaddon.mixin;

import com.aotaddon.util.SkillTreeGearHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "daot.BladeAttackTracker", remap = false)
public class BladeDamageSkillMixin {

    @Redirect(
            method = "tick(Lnet/minecraft/server/MinecraftServer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            require = 0,
            remap = false
    )
    private static boolean aotaddon$applyBladeSkillDamage(LivingEntity target, DamageSource source, float damage) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            damage *= SkillTreeGearHelper.getBladeDamageMultiplier(player);
        }
        boolean result = target.hurt(source, damage);
        if (result && target.isDeadOrDying() && attacker instanceof ServerPlayer player) {
            player.getPersistentData().putBoolean("aotaddon_blade_kill_xp", true);
        }
        return result;
    }
}