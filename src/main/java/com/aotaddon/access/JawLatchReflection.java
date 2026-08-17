package com.aotaddon.access;

import com.aotaddon.AotAddon;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;

/**
 * Reflective access to daot.TestShifterTitanEntity's private latch state
 * (DATA_LATCHED / DATA_LATCH_NORMAL_X / DATA_LATCH_NORMAL_Z / anchorX,Y,Z).
 *
 * Cancelling the vanilla tryLatch() call isn't enough on its own - the
 * jaw's own tick loop only pins position (anchorX/Y/Z snap) and stops
 * re-calling tryLatch() once isLatched() actually returns true, and the
 * chomp ability's own gating likely checks isLatched() too. So for
 * clinging-to-a-titan to look and behave like a real latch (not just
 * float in place), we have to flip DAOT's real synced DATA_LATCHED flag
 * and keep anchorX/Y/Z in sync ourselves each tick, since the titan we're
 * clinging to moves (unlike a wall).
 *
 * class_2940 (DAOT's declared field type) and EntityDataAccessor are the
 * same runtime class under Sinytra Connector - it remaps names, not the
 * class graph - so a plain Mojmap SynchedEntityData/EntityDataAccessor
 * cast is safe here even though DATA_LATCHED itself is only reachable via
 * reflection (daot.TestShifterTitanEntity isn't on the compile classpath).
 *
 * Static Field handles are cached once (like StaminaReflection), not
 * per-call - if the lookup ever fails (build mismatch) we log once and
 * every call after that becomes a safe no-op instead of retrying reflection
 * every tick.
 */
public final class JawLatchReflection {

    private static boolean initFailed = false;
    private static Field dataLatchedField;
    private static Field dataLatchNormalXField;
    private static Field dataLatchNormalZField;
    private static Field anchorXField;
    private static Field anchorYField;
    private static Field anchorZField;

    private JawLatchReflection() {}

    private static synchronized boolean ensureInit() {
        if (dataLatchedField != null) return true;
        if (initFailed) return false;
        try {
            Class<?> testShifterClass = Class.forName("daot.TestShifterTitanEntity");

            dataLatchedField = testShifterClass.getDeclaredField("DATA_LATCHED");
            dataLatchedField.setAccessible(true);

            dataLatchNormalXField = testShifterClass.getDeclaredField("DATA_LATCH_NORMAL_X");
            dataLatchNormalXField.setAccessible(true);

            dataLatchNormalZField = testShifterClass.getDeclaredField("DATA_LATCH_NORMAL_Z");
            dataLatchNormalZField.setAccessible(true);

            anchorXField = testShifterClass.getDeclaredField("anchorX");
            anchorXField.setAccessible(true);

            anchorYField = testShifterClass.getDeclaredField("anchorY");
            anchorYField.setAccessible(true);

            anchorZField = testShifterClass.getDeclaredField("anchorZ");
            anchorZField.setAccessible(true);

            return true;
        } catch (Exception e) {
            initFailed = true;
            AotAddon.LOGGER.error("[JawLatchReflection] failed to resolve daot latch fields - "
                    + "titan-clinging will silently no-op: {}", e.toString());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static void setLatched(LivingEntity jaw, boolean value) {
        if (!ensureInit()) return;
        try {
            EntityDataAccessor<Boolean> accessor = (EntityDataAccessor<Boolean>) dataLatchedField.get(null);
            SynchedEntityData data = jaw.getEntityData();
            data.set(accessor, value);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawLatchReflection] setLatched failed: {}", e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public static void setLatchNormal(LivingEntity jaw, float nx, float nz) {
        if (!ensureInit()) return;
        try {
            EntityDataAccessor<Float> ax = (EntityDataAccessor<Float>) dataLatchNormalXField.get(null);
            EntityDataAccessor<Float> az = (EntityDataAccessor<Float>) dataLatchNormalZField.get(null);
            SynchedEntityData data = jaw.getEntityData();
            data.set(ax, nx);
            data.set(az, nz);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawLatchReflection] setLatchNormal failed: {}", e.toString());
        }
    }

    public static void setAnchor(LivingEntity jaw, double x, double y, double z) {
        if (!ensureInit()) return;
        try {
            anchorXField.set(jaw, x);
            anchorYField.set(jaw, y);
            anchorZField.set(jaw, z);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[JawLatchReflection] setAnchor failed: {}", e.toString());
        }
    }
}