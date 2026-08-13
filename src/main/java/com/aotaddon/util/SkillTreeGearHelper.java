package com.aotaddon.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class SkillTreeGearHelper {
    private static final String KUBEJS_DATA_KEY = "KubeJSPersistentData";
    private static final String GEAR_TREE = "gear";

    private static final float DAMAGE_PER_NODE = 0.075f;
    private static final int BLADE_DAMAGE_NODE_COUNT = 8;
    private static final int APG_DAMAGE_NODE_COUNT = 8;
    private static final int SPEAR_CAPACITY_NODE_COUNT = 4;

    private SkillTreeGearHelper() {
    }

    public static float getBladeDamageMultiplier(ServerPlayer player) {
        int nodes = countUnlockedNodes(player, GEAR_TREE, "odm_", BLADE_DAMAGE_NODE_COUNT);
        return 1.0f + nodes * DAMAGE_PER_NODE;
    }

    public static float getApgDamageMultiplier(ServerPlayer player) {
        int nodes = countUnlockedNodes(player, GEAR_TREE, "apg_", APG_DAMAGE_NODE_COUNT);
        return 1.0f + nodes * DAMAGE_PER_NODE;
    }

    public static int getSpearCapacityBonus(ServerPlayer player) {
        return countUnlockedNodes(player, GEAR_TREE, "spear_", SPEAR_CAPACITY_NODE_COUNT);
    }

    private static int countUnlockedNodes(ServerPlayer player, String tree, String nodePrefix, int maxNodes) {
        int unlocked = 0;
        for (int i = 1; i <= maxNodes; i++) {
            if (hasNode(player, tree, nodePrefix + i)) {
                unlocked++;
            }
        }
        return unlocked;
    }

    private static boolean hasNode(ServerPlayer player, String tree, String node) {
        CompoundTag root = player.getPersistentData();
        String key = "st_node_" + tree + "_" + node;

        if (root.getCompound(KUBEJS_DATA_KEY).getBoolean(key)) {
            return true;
        }

        return root.getBoolean(key);
    }
}
