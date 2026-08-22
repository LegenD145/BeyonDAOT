package com.aotaddon.combat;

import com.aotaddon.network.HeadlessTitanSyncPayload;
import com.aotaddon.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Executes decapitation once the eye-hit handler confirms the hit qualifies.
 */
public final class DecapitationHandler {

    private DecapitationHandler() {
    }

    public static void decapitate(LivingEntity shifterTitan, ServerPlayer player) {
        Vec3 headPos = headWorldPosition(shifterTitan);
        String titanClass = shifterTitan.getClass().getSimpleName();

        if (shifterTitan.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    headPos.x, headPos.y, headPos.z, 30, 0.4, 0.4, 0.4, 0.05);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, // placeholder red-ish particle; swap for a real blood particle if one exists in daot/aotaddon
                    headPos.x, headPos.y, headPos.z, 40, 0.5, 0.3, 0.5, 0.1);

            PacketDistributor.sendToPlayersNear(level, null,
                    headPos.x, headPos.y, headPos.z, 128.0,
                    new HeadlessTitanSyncPayload(shifterTitan.getId()));

            SeveredPartEntity head = new SeveredPartEntity(ModEntities.SEVERED_PART.get(), level);
            head.moveTo(headPos.x, headPos.y, headPos.z, shifterTitan.getYRot(), 0.0f);
            head.setBoneName("head");
            head.setTitanClassName(titanClass);

            Vec3 outward = player.getLookAngle().scale(0.6).add(0, 0.4, 0);
            head.setDeltaMovement(outward);
            head.hasImpulse = true;

            level.addFreshEntity(head);
        }

        // Instant kill - same Float.MAX_VALUE pathway TitanEntity's own
        // nape/eye kill logic already uses internally.
        shifterTitan.hurt(shifterTitan.damageSources().generic(), Float.MAX_VALUE);
    }

    private static Vec3 headWorldPosition(LivingEntity titan) {
        return titan.position().add(0, ShifterTitanHelper.headWorldOffset(titan), 0);
    }
}
