package com.aotaddon.util;

import com.aotaddon.AotAddon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Cancels incoming projectile hits (vanilla arrows, daot's APG shots, and anything
 * else built as a real Projectile entity - NeoForge fires this event generically for
 * any of them) against a player who is currently inside their parry window.
 *
 * Cancelling ProjectileImpactEvent skips the projectile's own onHitEntity/onHitBlock
 * logic (no damage applied), but leaves the projectile continuing to travel under normal
 * physics - we additionally discard it here for a cleaner "deflected and gone" feel
 * rather than having it sail through and potentially re-trigger further down its path.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID)
public class ParryProjectileHandler {

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        HitResult result = event.getRayTraceResult();
        if (!(result instanceof EntityHitResult entityHit)) {
            return; // Only care about projectiles hitting an entity, not blocks.
        }

        Entity target = entityHit.getEntity();
        if (!(target instanceof ServerPlayer player)) {
            return; // Parry only protects players, not mobs/titans.
        }

        long currentTick = player.serverLevel().getServer().getTickCount();
        if (!ParryStateHandler.isInParryWindow(player.getUUID(), currentTick)) {
            return;
        }

        // Parried!
        event.setCanceled(true);

        Projectile projectile = event.getProjectile();
        ServerLevel level = player.serverLevel();

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);

        AotAddon.LOGGER.info("[Parry] {} parried a {} projectile",
                player.getName().getString(), projectile.getClass().getSimpleName());

        if (projectile instanceof Entity projectileEntity) {
            projectileEntity.discard();
        }
    }
}
