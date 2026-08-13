package com.aotaddon.titan;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomRetargetGoal extends Goal {

    private final Mob titan;
    private final double range;
    private final float switchChancePerTick;
    private final Random random = new Random();

    public RandomRetargetGoal(Mob titan, double range, float switchChancePerTick) {
        this.titan = titan;
        this.range = range;
        this.switchChancePerTick = switchChancePerTick;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    private List<Player> findValidTargets() {
        AABB box = titan.getBoundingBox().inflate(range);
        return titan.level().getEntitiesOfClass(
                Player.class,
                box,
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
        );
    }

    @Override
    public boolean canUse() {
        if (titan.getTarget() != null) {
            return false;
        }
        return !findValidTargets().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = titan.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        return titan.distanceToSqr(target) <= range * range;
    }

    @Override
    public void start() {
        List<Player> candidates = findValidTargets();
        if (!candidates.isEmpty()) {
            titan.setTarget(candidates.get(random.nextInt(candidates.size())));
        }
    }

    @Override
    public void tick() {
        if (random.nextFloat() >= switchChancePerTick) {
            return;
        }
        LivingEntity current = titan.getTarget();
        List<Player> candidates = findValidTargets().stream()
                .filter(p -> p != current)
                .collect(Collectors.toList());
        if (!candidates.isEmpty()) {
            titan.setTarget(candidates.get(random.nextInt(candidates.size())));
        }
    }

    @Override
    public void stop() {
        titan.setTarget(null);
    }
}