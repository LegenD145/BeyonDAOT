package com.aotaddon.campfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared lit-campfire scan used by regen, the sit prompt, and sit packet validation.
 */
public final class CampfireHelper {

    public static final int SEARCH_RADIUS_HORIZONTAL = 5;
    public static final int SEARCH_RADIUS_VERTICAL = 2;

    private CampfireHelper() {}

    public static boolean isLitCampfire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock
                && state.hasProperty(CampfireBlock.LIT)
                && state.getValue(CampfireBlock.LIT);
    }

    public static BlockPos findNearestLit(Level level, BlockPos center) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (int dx = -SEARCH_RADIUS_HORIZONTAL; dx <= SEARCH_RADIUS_HORIZONTAL; dx++) {
            for (int dy = -SEARCH_RADIUS_VERTICAL; dy <= SEARCH_RADIUS_VERTICAL; dy++) {
                for (int dz = -SEARCH_RADIUS_HORIZONTAL; dz <= SEARCH_RADIUS_HORIZONTAL; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!isLitCampfire(level.getBlockState(pos))) {
                        continue;
                    }
                    double dist = center.distSqr(pos);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = pos.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    public static boolean isNearLitCampfire(Level level, BlockPos center) {
        return findNearestLit(level, center) != null;
    }
}
