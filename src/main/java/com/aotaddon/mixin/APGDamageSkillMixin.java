package com.aotaddon.mixin;

import com.aotaddon.util.SkillTreeGearHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
//credit me on discord thewillofpur if you wish to use any of my content
@Mixin(targets = "daot.APGProjectileEntity", remap = false)
public class APGDamageSkillMixin {

    @Redirect(
            method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            require = 0,
            remap = false
    )
    private boolean aotaddon$applyApgSkillDamage(Entity target, DamageSource source, float damage) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            damage *= SkillTreeGearHelper.getApgDamageMultiplier(player);
        }
        boolean result = target.hurt(source, damage);
        if (result && target instanceof LivingEntity le && le.isDeadOrDying() && attacker instanceof ServerPlayer player) {
            player.getPersistentData().putBoolean("aotaddon_apg_kill_xp", true);
        }
        return result;
    }
}