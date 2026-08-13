package com.aotaddon.util;

import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants ODM skill-tree XP based on gas consumed.
 *
 * This intentionally matches SkillTreeArmorXP's storage style: values are
 * stored inside the player's KubeJSPersistentData compound so KubeJS reads the
 * same st_xp_odm, st_level_odm, and st_points_odm keys.
 */
public class OdmXpHandler {

    private static final int[] XP_PER_LEVEL = {
            100, 250, 450, 550, 650, 700, 825, 1025, 1125, 1200,
            1275, 1325, 1425, 1525, 1625, 1725, 1825, 2026, 2255, 2500
    };

    private static final int MAX_LEVEL = 20;
    private static final String TREE_NAME = "odm";
    private static final String TREE_LABEL = "ODM";

    public static void grantXp(ServerPlayer player, float amount) {
        addXp(player, amount);
    }

    public static void grantGasXp(ServerPlayer player, int gasConsumed) {
        if (gasConsumed <= 0) return;

        float xpPerUnit;
        try {
            xpPerUnit = AddonConfig.ODM_XP_PER_GAS_UNIT.get().floatValue();
        } catch (IllegalStateException e) {
            xpPerUnit = 0.15f;
        }

        addXp(player, gasConsumed * xpPerUnit);
    }

    private static void addXp(ServerPlayer player, float amount) {
        if (amount <= 0.0f) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.getCompound("KubeJSPersistentData");

        int level = data.getInt("st_level_" + TREE_NAME);
        if (level >= MAX_LEVEL) return;

        float xp = data.getFloat("st_xp_" + TREE_NAME) + amount;

        while (level < MAX_LEVEL) {
            float needed = XP_PER_LEVEL[level];
            if (xp < needed) break;

            xp -= needed;
            level++;
            data.putInt("st_level_" + TREE_NAME, level);

            int points = data.getInt("st_points_" + TREE_NAME) + 1;
            data.putInt("st_points_" + TREE_NAME, points);

            player.sendSystemMessage(Component.literal(
                    "[Skill Tree] " + TREE_LABEL + " leveled up to " + level +
                            "! You have " + points + " upgrade points."
            ));

            AotAddon.LOGGER.debug("[OdmXp] {} leveled ODM to {} ({} pts)",
                    player.getName().getString(), level, points);

            if (level >= MAX_LEVEL) {
                xp = 0.0f;
                break;
            }
        }

        data.putFloat("st_xp_" + TREE_NAME, xp);
        root.put("KubeJSPersistentData", data);
    }
}
