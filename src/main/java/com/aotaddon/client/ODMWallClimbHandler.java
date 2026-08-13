package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
// saem here too btw chuds
/**
 * Handles W/S movement while the player is hooked via ODM, without consuming gas.
 *
 * Key design: we do NOT call player.setDeltaMovement() here.
 * Instead we store the desired velocity in pendingVelocity.
 * ODMGasSuppressionMixin then applies it at the tail of applyHookMovement,
 * AFTER Danny has set his velocity — so ours wins.
 */
@OnlyIn(Dist.CLIENT)
public class ODMWallClimbHandler {

    private static boolean wasPerching = false;
    private static Vec3 pendingVelocity = null;

    private static boolean loggedHookError = false;
    private static boolean loggedPosError  = false;
    private static boolean loggedODMError  = false;

    public static void tick() {
        pendingVelocity = null;

        boolean enabled;
        double climbSpeed, descendSpeed;
        try {
            enabled      = AddonConfig.ODM_WALL_CLIMB_ENABLED.get();
            climbSpeed   = AddonConfig.ODM_WALL_CLIMB_SPEED.get();
            descendSpeed = AddonConfig.ODM_WALL_DESCEND_SPEED.get();
        } catch (IllegalStateException e) {
            wasPerching = false;
            return;
        }

        if (!enabled) { wasPerching = false; return; }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { wasPerching = false; return; }
        if (!isWearingODM(player)) { wasPerching = false; return; }

        boolean leftLatched  = isHookLatched(true);
        boolean rightLatched = isHookLatched(false);
        boolean hooked = leftLatched || rightLatched;

        if (!hooked) { wasPerching = false; return; }

        boolean isBoosting = mc.options.keyJump.isDown();
        boolean pressingA  = mc.options.keyLeft.isDown();
        boolean pressingD  = mc.options.keyRight.isDown();
        boolean pressingW  = mc.options.keyUp.isDown();
        boolean pressingS  = mc.options.keyDown.isDown();

        boolean perching = hooked && !isBoosting && !pressingA && !pressingD && !player.onGround();
        wasPerching = perching;

        if (!perching) return;
        if (!pressingW && !pressingS) {
            // Dampen — store zero-ish velocity so mixin can apply it
            Vec3 vel = player.getDeltaMovement();
            pendingVelocity = new Vec3(vel.x * 0.5, vel.y * 0.5, vel.z * 0.5);
            return;
        }

        // Use the highest hook as the anchor — avoids the two-hook freeze
        Vec3 hookPos = getBestHookPos(leftLatched, rightLatched);
        if (hookPos == null) return;

        Vec3 playerPos = player.position();
        // Use current velocity from player — Danny hasn't overwritten it yet this tick
        Vec3 current = player.getDeltaMovement();

        if (pressingW) {
            Vec3 toHook   = hookPos.subtract(playerPos).normalize();
            Vec3 climbDir = new Vec3(toHook.x * 0.25, Math.max(toHook.y, 0.7), toHook.z * 0.25).normalize();
            pendingVelocity = new Vec3(
                    current.x * 0.4 + climbDir.x * climbSpeed,
                    current.y * 0.4 + climbDir.y * climbSpeed,
                    current.z * 0.4 + climbDir.z * climbSpeed
            );
        } else {
            Vec3 fromHook   = playerPos.subtract(hookPos).normalize();
            Vec3 descendDir = new Vec3(fromHook.x * 0.2, Math.min(fromHook.y, -0.5), fromHook.z * 0.2).normalize();
            pendingVelocity = new Vec3(
                    current.x * 0.4 + descendDir.x * descendSpeed,
                    current.y * 0.4 + descendDir.y * descendSpeed,
                    current.z * 0.4 + descendDir.z * descendSpeed
            );
        }
    }

    public static boolean isCurrentlyPerching() { return wasPerching; }
    public static Vec3 getPendingVelocity()      { return pendingVelocity; }
    public static void clearPendingVelocity()    { pendingVelocity = null; }

    // -------------------------------------------------------------------------
    // Pick the highest hook — avoids two-hook momentum cancellation
    // -------------------------------------------------------------------------

    private static Vec3 getBestHookPos(boolean leftLatched, boolean rightLatched) {
        try {
            Class<?> tickHandler = Class.forName("daot.ODMTickHandler");
            Class<?> hookClass   = Class.forName("daot.HookPoint");
            java.lang.reflect.Field posField = hookClass.getField("position");

            Vec3 leftPos = null, rightPos = null;

            if (leftLatched) {
                Object hook = tickHandler.getMethod("getLeftHook").invoke(null);
                if (hook != null) {
                    Object pos = posField.get(hook);
                    if (pos instanceof Vec3 v) leftPos = v;
                }
            }
            if (rightLatched) {
                Object hook = tickHandler.getMethod("getRightHook").invoke(null);
                if (hook != null) {
                    Object pos = posField.get(hook);
                    if (pos instanceof Vec3 v) rightPos = v;
                }
            }

            if (leftPos != null && rightPos != null)
                return leftPos.y >= rightPos.y ? leftPos : rightPos;
            if (leftPos  != null) return leftPos;
            if (rightPos != null) return rightPos;
            return null;
        } catch (Exception e) {
            if (!loggedPosError) {
                AotAddon.LOGGER.error("[ODMWallClimb] getBestHookPos failed: {}", e.toString());
                loggedPosError = true;
            }
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Hook latched check
    // -------------------------------------------------------------------------

    private static boolean isHookLatched(boolean left) {
        try {
            Class<?> tickHandler = Class.forName("daot.ODMTickHandler");
            Object hook = tickHandler.getMethod(left ? "getLeftHook" : "getRightHook").invoke(null);
            if (hook == null) return false;
            Class<?> hookClass = Class.forName("daot.HookPoint");
            boolean active     = hookClass.getField("active").getBoolean(hook);
            boolean extending  = hookClass.getField("isExtending").getBoolean(hook);
            boolean retracting = hookClass.getField("isRetracting").getBoolean(hook);
            return active && !extending && !retracting;
        } catch (Exception e) {
            if (!loggedHookError) {
                AotAddon.LOGGER.error("[ODMWallClimb] isHookLatched failed: {}", e.toString());
                loggedHookError = true;
            }
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // ODM gear check
    // -------------------------------------------------------------------------

    private static boolean isWearingODM(LocalPlayer player) {
        try {
            net.minecraft.world.item.ItemStack legs = player.getItemBySlot(
                    net.minecraft.world.entity.EquipmentSlot.LEGS);
            if (legs.isEmpty()) return false;
            Class<?> daot = Class.forName("daot.DannysAot");
            return (boolean) daot.getMethod("isODMGear", net.minecraft.world.item.Item.class)
                    .invoke(null, legs.getItem());
        } catch (Exception e) {
            if (!loggedODMError) {
                AotAddon.LOGGER.error("[ODMWallClimb] isWearingODM failed: {}", e.toString());
                loggedODMError = true;
            }
            return false;
        }
    }
}
