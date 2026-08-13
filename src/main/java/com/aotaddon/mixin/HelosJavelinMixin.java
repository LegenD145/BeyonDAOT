package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.family.FamilyData;
import com.aotaddon.family.FamilyEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into Spartan Weaponry's ThrowingWeaponEntity.onHitEntity.
 *
 * IMPORTANT: No direct imports of Entity/LivingEntity/AbstractArrow/ServerLevel —
 * those chain-load net.minecraft.world.entity.Entity at class load time and break
 * TRansition's mixin window under Sinytra Connector. All entity references are
 * kept inline inside method bodies only.
 */
@Mixin(targets = "org.xiyu.spartanweaponryunofficial.entity.projectile.ThrowingWeaponEntity", remap = false)
public class HelosJavelinMixin {

    @Inject(
            method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void onJavelinHitEntity(EntityHitResult hitResult, CallbackInfo ci) {
        try {
            net.minecraft.world.entity.Entity hitEntity = hitResult.getEntity();

            // Cast to AbstractArrow inline — no top-level import
            net.minecraft.world.entity.Entity thrower =
                    ((net.minecraft.world.entity.projectile.AbstractArrow)(Object) this).getOwner();

            if (!(thrower instanceof ServerPlayer player)) return;
            if (!FamilyData.isHelos(player)) return;
            if (!(hitEntity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;

            Vec3 hitPos = hitResult.getLocation();

            // ── 1. Explosion ──────────────────────────────────────────────────
            try {
                level.explode(
                        null,
                        hitPos.x, hitPos.y, hitPos.z,
                        2.0f,
                        false,
                        net.minecraft.world.level.Level.ExplosionInteraction.NONE
                );
            } catch (Exception e) {
                AotAddon.LOGGER.error("[TitanRequiem] Javelin explosion failed: {}", e.getMessage());
            }

            // ── 2. Nape / Eye instant kill ────────────────────────────────────
            if (FamilyEventHandler.isNapeOrEye(hitEntity)) {
                try {
                    hitEntity.kill();
                    AotAddon.LOGGER.debug("[TitanRequiem] Nape/eye killed by {} javelin",
                            player.getName().getString());
                } catch (Exception e) {
                    AotAddon.LOGGER.error("[TitanRequiem] Nape/eye kill failed: {}", e.getMessage());
                }
                return;
            }

            // ── 3. Pure titan hit — wipe if charge full ───────────────────────
            if (FamilyEventHandler.isPureTitan(hitEntity)) {
                if (FamilyData.isHelosReady(player)) {
                    try {
                        if (hitEntity instanceof net.minecraft.world.entity.LivingEntity living) {
                            living.kill();
                        }
                    } catch (Exception e) {
                        AotAddon.LOGGER.error("[TitanRequiem] Wipe trigger kill failed: {}", e.getMessage());
                    }
                    FamilyEventHandler.executeHelösWipe(player, hitPos.x, hitPos.y, hitPos.z);
                }
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[TitanRequiem] HelosJavelinMixin outer catch: {}", e.getMessage());
        }
    }
}