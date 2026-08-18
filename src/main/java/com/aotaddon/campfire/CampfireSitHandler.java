package com.aotaddon.campfire;

import com.aotaddon.registry.ModEntities;
import com.aotaddon.util.CombatTagHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns an invisible seat beside the nearest lit campfire and mounts the player.
 */
public final class CampfireSitHandler {

    private static final double SEAT_OFFSET = 2.5;

    private CampfireSitHandler() {}

    public static void trySit(ServerPlayer player) {
        if (player.isPassenger() || player.isVehicle()) {
            return;
        }
        if (!player.onGround()) {
            return;
        }
        if (CombatTagHandler.isInCombat(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos fire = CampfireHelper.findNearestLit(level, player.blockPosition());
        if (fire == null) {
            return;
        }

        Vec3 fireCenter = Vec3.atBottomCenterOf(fire).add(0, 0.05, 0);
        Vec3 away = new Vec3(player.getX() - fireCenter.x, 0, player.getZ() - fireCenter.z);
        if (away.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            away = new Vec3(look.x, 0, look.z);
        }
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1, 0, 0);
        }
        Vec3 dir = away.normalize();
        Vec3 seatPos = fireCenter.add(dir.scale(SEAT_OFFSET));

        CampfireSeatEntity seat = ModEntities.CAMPFIRE_SEAT.get().create(level);
        if (seat == null) {
            return;
        }
        seat.moveTo(seatPos.x, fire.getY(), seatPos.z, yawToward(seatPos, fireCenter), 0);
        level.addFreshEntity(seat);
        player.startRiding(seat, true);
        player.setYRot(seat.getYRot());
        player.setYHeadRot(seat.getYRot());
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
    }
}
