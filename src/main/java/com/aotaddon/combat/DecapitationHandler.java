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
 * Executes the actual outcome of a Female Titan decapitation once
 * FemaleDecapitationHandler has confirmed the hit qualifies.
 *
 * Spawns a SeveredPartEntity showing only Female Titan's "head" bone (and
 * its children - mouth, hair, nape_hitbox, eye_hitbox per her geo file),
 * left in bind pose, given an outward+upward impulse based on the
 * attacking player's look direction, then falls under gravity and
 * despawns after its lifespan. Kills the shifter outright.
 *
 * headWorldPosition() uses daot's known Female Titan head height for the
 * current command path. Other titan types still fall back to a rough
 * percentage of bounding-box height until their real animated bone anchors
 * are wired in.
 */
public final class DecapitationHandler {

    private static final double FEMALE_TITAN_HEAD_HEIGHT = 11.0;

    private DecapitationHandler() {
    }

    public static void decapitate(LivingEntity shifterTitan, ServerPlayer player) {
        Vec3 headPos = headWorldPosition(shifterTitan);

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
        if (titan.getClass().getSimpleName().equals("FemaleTitanEntity")) {
            return titan.position().add(0, FEMALE_TITAN_HEAD_HEIGHT, 0);
        }

        double headHeightFraction = 0.9; // rough stand-in until real head-bone world pos is wired in
        return titan.position().add(0, titan.getBbHeight() * headHeightFraction, 0);
    }
}
