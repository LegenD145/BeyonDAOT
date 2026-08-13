package com.aotaddon.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.joml.Vector3f;

/**
 * A severed titan body part, spawned by DecapitationHandler. Reuses the
 * parent titan's existing GeckoLib model/texture client-side (see
 * SeveredPartRenderer + SeveredPartGeoModel) rather than needing a separate
 * exported model - the renderer shows only the bone named in BONE_NAME and
 * its children, left in bind pose since this entity never drives any
 * animation controllers.
 *
 * Physics is hand-rolled here rather than going through GeckoLib at all -
 * gravity + ground collision, matching vanilla FallingBlock/ItemEntity
 * style, despawning after a fixed lifespan.
 *
 * Implements GeoEntity with an empty registerControllers(), the same
 * "no animation, stays in bind pose" pattern already proven working for
 * ShifterUnlockPotionItem's GeoItem implementation.
 */
public class SeveredPartEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    private static final EntityDataAccessor<String> BONE_NAME =
            SynchedEntityData.defineId(SeveredPartEntity.class, EntityDataSerializers.STRING);

    private static final double GRAVITY = 0.04;
    private static final double CULLING_RADIUS = 8.0;
    private static final double MAX_RENDER_DISTANCE = 128.0;
    private static final int LIFESPAN_TICKS = 20 * 12; // 12 seconds
    private static final int HEAVY_BLEED_TICKS = 20 * 4;
    private static final double BLOOD_ORBIT_RADIUS = 0.55;
    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.55f, 0.0f, 0.0f), 1.35f);

    private int age = 0;
    private boolean landed = false;
    private float landedSpinDegrees = 0.0f;

    public SeveredPartEntity(EntityType<? extends SeveredPartEntity> type, Level level) {
        super(type, level);
    }

    public void setBoneName(String boneName) {
        this.entityData.set(BONE_NAME, boneName);
    }

    public String getBoneName() {
        return this.entityData.get(BONE_NAME);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BONE_NAME, "head");
    }

    @Override
    public void tick() {
        super.tick();

        if (landed) {
            setDeltaMovement(Vec3.ZERO);
            ageAndDiscardIfExpired();
            return;
        }

        Vec3 motion = getDeltaMovement();
        if (!onGround()) {
            motion = motion.subtract(0, GRAVITY, 0);
        }

        move(MoverType.SELF, motion);

        if (onGround()) {
            markLanded();
            motion = Vec3.ZERO;
        } else {
            spawnSpinBloodTrail();
        }
        setDeltaMovement(motion);

        ageAndDiscardIfExpired();
    }

    private void markLanded() {
        landed = true;
        landedSpinDegrees = rawSpinDegrees(0.0f);
        hasImpulse = false;
    }

    private void ageAndDiscardIfExpired() {
        age++;
        if (age >= LIFESPAN_TICKS) {
            discard();
        }
    }

    private void spawnSpinBloodTrail() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        double spin = getSpinRadians(0.0f);
        emitBloodAtSpinPoint(level, spin, 0.0);
        emitBloodAtSpinPoint(level, spin + Math.PI, -0.1);

        if (age < HEAVY_BLEED_TICKS || age % 2 == 0) {
            double mistX = getX() + Math.cos(spin + Math.PI * 0.5) * 0.35;
            double mistY = getY() + 0.15 + Math.sin(spin * 0.7) * 0.2;
            double mistZ = getZ() + Math.sin(spin + Math.PI * 0.5) * 0.35;
            level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    mistX, mistY, mistZ, age < HEAVY_BLEED_TICKS ? 2 : 1,
                    0.05, 0.05, 0.05, 0.015);
        }

        if (age % 5 == 0) {
            level.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY() + 0.1, getZ(), 1,
                    0.25, 0.12, 0.25, 0.01);
        }
    }

    private void emitBloodAtSpinPoint(ServerLevel level, double spin, double yOffset) {
        double x = getX() + Math.cos(spin) * BLOOD_ORBIT_RADIUS;
        double y = getY() + yOffset + Math.sin(spin * 1.4) * 0.25;
        double z = getZ() + Math.sin(spin) * BLOOD_ORBIT_RADIUS;
        int count = age < HEAVY_BLEED_TICKS ? 2 : 1;

        level.sendParticles(BLOOD_DUST, x, y, z, count, 0.035, 0.04, 0.035, 0.02);

        if (age % 3 == 0) {
            level.sendParticles(ParticleTypes.FALLING_LAVA, x, y - 0.15, z, 1, 0.02, 0.0, 0.02, 0.0);
        }
    }

    public float getSpinDegrees(float partialTick) {
        if (landed) {
            return landedSpinDegrees;
        }
        return rawSpinDegrees(partialTick);
    }

    private float rawSpinDegrees(float partialTick) {
        return (tickCount + partialTick) * 34.0f;
    }

    public double getSpinRadians(float partialTick) {
        return Math.toRadians(getSpinDegrees(partialTick));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BoneName")) {
            setBoneName(tag.getString("BoneName"));
        }
        age = tag.getInt("Age");
        landed = tag.getBoolean("Landed");
        landedSpinDegrees = tag.getFloat("LandedSpinDegrees");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("BoneName", getBoneName());
        tag.putInt("Age", age);
        tag.putBoolean("Landed", landed);
        tag.putFloat("LandedSpinDegrees", landedSpinDegrees);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(
                getX() - CULLING_RADIUS, getY() - CULLING_RADIUS, getZ() - CULLING_RADIUS,
                getX() + CULLING_RADIUS, getY() + CULLING_RADIUS, getZ() + CULLING_RADIUS
        );
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Intentionally empty - no animation controllers registered, so
        // every bone keeps its default bind-pose transform from the geo
        // file. This is what makes the "static, unanimated" severed part
        // trick work.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
