package com.aotaddon.client;
// so i made ai explain whats happening on here so if some important dude wants to check stuff they could see whats going on
import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;
import com.aotaddon.network.DodgeStartPayload;
import com.aotaddon.network.SkillEffectPayload;
import com.aotaddon.network.TrailEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles the two AOT Freedom War-style ODM skills:
 *
 * IMPULSE — double-tap W. Dashes in the full 3D look direction (pitch + yaw).
 *   Used to build extra speed for nape slashes and big-distance combos.
 *   Longer cooldown, higher gas cost.
 *
 * DODGE — double-tap S. Always dashes backward relative to the player's
 *   facing, regardless of look pitch. Grants a brief window of damage
 *   immunity (server-side, see DodgeIFrameHandler) but does NOT break free
 *   from an existing grab — that's reserved for a future skill.
 *   Shorter cooldown, cheap gas cost.
 *
 * Both skills spawn a burst particle on activation and a short decaying
 * trail while the dash velocity bleeds off. Effects are sent to the server
 * so all nearby players can see them, not just the one performing the skill.
 */
@OnlyIn(Dist.CLIENT)
public class ODMSkillHandler {

    // Last press timestamp per key (epoch ms, 0 = never)
    private static long lastW = 0;
    private static long lastS = 0;

    // Key state last tick — for rising-edge detection
    private static boolean prevW = false;
    private static boolean prevS = false;

    // Cooldown trackers — separate per skill
    private static long lastImpulseTime = 0;
    private static long lastDodgeTime   = 0;

    // Trail state — counts down while active
    private static int trailTicksRemaining = 0;

    // -------------------------------------------------------------------------
    // Public tick entry-point
    // -------------------------------------------------------------------------

    public static void tick() {
        boolean enabled;
        long window;
        try {
            enabled = AddonConfig.ODM_SKILLS_ENABLED.get();
            window  = AddonConfig.ODM_SKILL_WINDOW_MS.get();
        } catch (IllegalStateException e) {
            resetPrev();
            return;
        }

        // Trail ticks down regardless of gear/state so it finishes naturally
        tickTrail();

        if (!enabled) { resetPrev(); return; }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { resetPrev(); return; }
        if (!isWearingODM(player)) { resetPrev(); return; }

        long now = System.currentTimeMillis();

        boolean curW = mc.options.keyUp.isDown();
        boolean curS = mc.options.keyDown.isDown();

        if (curW && !prevW) {
            if (now - lastW <= window) { tryImpulse(player, now); lastW = 0; }
            else lastW = now;
        }
        if (curS && !prevS) {
            if (now - lastS <= window) { tryDodge(player, now); lastS = 0; }
            else lastS = now;
        }

        prevW = curW;
        prevS = curS;
    }

    // -------------------------------------------------------------------------
    // Impulse
    // -------------------------------------------------------------------------

    private static void tryImpulse(LocalPlayer player, long now) {
        long cooldown;
        double strength;
        int gasCost;
        try {
            cooldown = AddonConfig.ODM_IMPULSE_COOLDOWN_MS.get();
            strength = AddonConfig.ODM_IMPULSE_STRENGTH.get();
            gasCost  = AddonConfig.ODM_IMPULSE_GAS_COST.get();
        } catch (IllegalStateException e) { return; }

        if (now - lastImpulseTime < cooldown) return;
        if (!hasGas(player)) return;

        consumeGas(player, gasCost);

        // Full 3D look direction — pitch and yaw both apply
        Vec3 look = player.getLookAngle().normalize();
        Vec3 dashVec = look.scale(strength);

        player.setDeltaMovement(player.getDeltaMovement().add(dashVec));
        lastImpulseTime = now;
        SkillCooldownTracker.start("Impulse", cooldown);

        spawnBurst(0);
        startTrail();

        AotAddon.LOGGER.debug("[ODMSkill] Impulse fired for {}", player.getName().getString());
    }

    // -------------------------------------------------------------------------
    // Dodge
    // -------------------------------------------------------------------------

    private static void tryDodge(LocalPlayer player, long now) {
        long cooldown;
        double strength;
        int gasCost;
        try {
            cooldown = AddonConfig.ODM_DODGE_COOLDOWN_MS.get();
            strength = AddonConfig.ODM_DODGE_STRENGTH.get();
            gasCost  = AddonConfig.ODM_DODGE_GAS_COST.get();
        } catch (IllegalStateException e) { return; }

        if (now - lastDodgeTime < cooldown) return;
        if (!hasGas(player)) return;

        consumeGas(player, gasCost);

        // Always backward relative to facing — horizontal only, ignores pitch
        float yawRad = (float) Math.toRadians(player.getYRot());
        double backX =  Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        Vec3 dashVec = new Vec3(backX, 0, backZ).normalize().scale(strength);

        player.setDeltaMovement(player.getDeltaMovement().add(dashVec));
        lastDodgeTime = now;
        SkillCooldownTracker.start("Dodge", cooldown);

        // Grant server-side i-frames
        PacketDistributor.sendToServer(new DodgeStartPayload());

        spawnBurst(1);
        startTrail();

        AotAddon.LOGGER.debug("[ODMSkill] Dodge fired for {}", player.getName().getString());
    }

    // -------------------------------------------------------------------------
    // Effects
    // -------------------------------------------------------------------------

    private static void spawnBurst(int skillId) {
        PacketDistributor.sendToServer(new SkillEffectPayload(skillId));
    }

    private static void startTrail() {
        int duration;
        try {
            duration = AddonConfig.ODM_TRAIL_DURATION_TICKS.get();
        } catch (IllegalStateException e) {
            duration = 7;
        }
        trailTicksRemaining = duration;
    }

    private static void tickTrail() {
        if (trailTicksRemaining <= 0) return;
        trailTicksRemaining--;
        PacketDistributor.sendToServer(new TrailEffectPayload());
    }

    // -------------------------------------------------------------------------
    // Danny's mod integration via reflection
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
            return false;
        }
    }

    private static boolean hasGas(LocalPlayer player) {
        try {
            net.minecraft.world.item.ItemStack legs = player.getItemBySlot(
                    net.minecraft.world.entity.EquipmentSlot.LEGS);
            Class<?> daot = Class.forName("daot.DannysAot");
            return (boolean) daot.getMethod("gearHasGas",
                            net.minecraft.world.item.ItemStack.class,
                            net.minecraft.world.entity.player.Player.class)
                    .invoke(null, legs, player);
        } catch (Exception e) {
            return true;
        }
    }

    private static void consumeGas(LocalPlayer player, int cost) {
        try {
            net.minecraft.world.item.ItemStack legs = player.getItemBySlot(
                    net.minecraft.world.entity.EquipmentSlot.LEGS);
            Class<?> daot = Class.forName("daot.DannysAot");
            daot.getMethod("consumeGasFromGear",
                            net.minecraft.world.item.ItemStack.class,
                            int.class,
                            net.minecraft.world.entity.player.Player.class)
                    .invoke(null, legs, cost, player);
        } catch (Exception e) {
            AotAddon.LOGGER.debug("[ODMSkill] Gas consume reflection failed: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void resetPrev() {
        prevW = false;
        prevS = false;
    }
}