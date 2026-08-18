package com.aotaddon.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "titanreqiuem")
public class SkillTreeArmorXP {

    private static final int MAX_LEVEL = 20;
    private static final float[] XP_PER_LEVEL = {
            100, 250, 450, 550, 650, 700, 825, 1025, 1125, 1200,
            1275, 1325, 1425, 1525, 1625, 1725, 1825, 2026, 2255, 2500
    };

    @SubscribeEvent
    public static void onPlayerDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        float damage = event.getNewDamage();
        if (damage <= 0) return;

        addArmorXP(player, damage * 0.5f);
    }

    private static void addArmorXP(ServerPlayer player, float amount) {
        CompoundTag root = player.getPersistentData();
        CompoundTag data = root.getCompound("KubeJSPersistentData");

        String treeName = "armor";
        String treeLabel = "Armor";

        int level = data.getInt("st_level_" + treeName);
        if (level >= MAX_LEVEL) return;

        float xp = data.getFloat("st_xp_" + treeName) + amount;

        while (level < MAX_LEVEL) {
            float needed = XP_PER_LEVEL[level];
            if (xp < needed) break;

            xp -= needed;
            level++;
            data.putInt("st_level_" + treeName, level);

            int points = data.getInt("st_points_" + treeName) + 1;
            data.putInt("st_points_" + treeName, points);

            player.sendSystemMessage(Component.literal(
                    "[Skill Tree] " + treeLabel + " leveled up to " + level +
                            "! You have " + points + " upgrade points."
            ));

            if (level >= MAX_LEVEL) {
                xp = 0;
                break;
            }
        }

        data.putFloat("st_xp_" + treeName, xp);
        root.put("KubeJSPersistentData", data);
    }
}