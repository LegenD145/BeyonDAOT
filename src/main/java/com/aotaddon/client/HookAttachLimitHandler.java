package com.aotaddon.client;

import com.aotaddon.AotAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Tracks how long each ODM hook (left/right) has been continuously latched onto something.
 * At 9 seconds (LIMIT_TICKS) that side is force-released, then must sit through an equal-length
 * cooldown before it can fire again. Both phases share one 0->LIMIT_TICKS counter per side —
 * "full" always means "the thing happens now" (force-detach during the attach phase, "ready to
 * fire" during cooldown), matching vanilla's sword-sweep indicator direction per Bodi's spec.
 *
 * DAOT is not a compile-time dependency of this addon (see AotAddon conventions: DAOT internals
 * are accessed via reflection, no cached static fields, everything in local try/catch), so every
 * lookup here is done fresh via Class.forName/getMethod rather than cached Method/Field handles.
 * This does mean paying reflection-lookup cost every client tick (20/sec) rather than once — if
 * that ever shows up as a real cost in profiling, caching the resolved Method/Field objects
 * (safe to cache, since they're tied to the Class itself, not any per-world HookPoint instance)
 * would be the first optimization to reach for.
 *
 * Design note: DAOT's HookPoint exposes both startRetract(Vec3) (plays the normal multi-tick
 * retract animation) and release() (instant, no animation). This uses release() — it needs no
 * extra Vec3 argument to construct via reflection, and it's the more literal match for "auto
 * detach, hook releases, you fall/glide free" as originally specced, versus starting a retract
 * animation that behaves like a manual retraction.
 */
public class HookAttachLimitHandler {

    private static final String DAOT_TICK_HANDLER = "daot.ODMTickHandler";
    private static final String DAOT_HOOK_POINT = "daot.HookPoint";
    private static final String DAOT_SOUND_MANAGER = "daot.ODMSoundManager";

    private static final int LIMIT_TICKS = 180; // 9 seconds at 20 ticks/sec

    private enum Phase {
        IDLE,           // hook not latched, bar hidden
        FILLING,        // latched, bar filling toward force-detach
        COOLING_DOWN    // force-detached, bar filling toward "ready to fire"
    }

    private static Phase leftPhase = Phase.IDLE;
    private static Phase rightPhase = Phase.IDLE;
    private static int leftTicks = 0;
    private static int rightTicks = 0;

    /** 0.0 (hidden/empty) to 1.0 (full) — HUD reads this directly for the left bar. */
    public static float getLeftFill() {
        return leftPhase == Phase.IDLE ? 0f : (float) leftTicks / LIMIT_TICKS;
    }

    /** 0.0 (hidden/empty) to 1.0 (full) — HUD reads this directly for the right bar. */
    public static float getRightFill() {
        return rightPhase == Phase.IDLE ? 0f : (float) rightTicks / LIMIT_TICKS;
    }

    public static boolean isLeftBarVisible() {
        return leftPhase != Phase.IDLE;
    }

    public static boolean isRightBarVisible() {
        return rightPhase != Phase.IDLE;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            leftPhase = Phase.IDLE;
            rightPhase = Phase.IDLE;
            leftTicks = 0;
            rightTicks = 0;
            return;
        }

        try {
            Class<?> tickHandlerClass = Class.forName(DAOT_TICK_HANDLER);
            Class<?> hookPointClass = Class.forName(DAOT_HOOK_POINT);

            Method getLeftHook = tickHandlerClass.getMethod("getLeftHook");
            Method getRightHook = tickHandlerClass.getMethod("getRightHook");

            Field activeField = hookPointClass.getField("active");
            Field extendingField = hookPointClass.getField("isExtending");
            Field retractingField = hookPointClass.getField("isRetracting");
            Method releaseMethod = hookPointClass.getMethod("release");

            Object leftHook = getLeftHook.invoke(null);
            Object rightHook = getRightHook.invoke(null);

            leftTicks = tickSide(leftHook, activeField, extendingField, retractingField,
                    releaseMethod, leftPhase, leftTicks, side -> leftPhase = side);
            rightTicks = tickSide(rightHook, activeField, extendingField, retractingField,
                    releaseMethod, rightPhase, rightTicks, side -> rightPhase = side);
        } catch (ClassNotFoundException e) {
            // DAOT not present on this install — nothing to track.
        } catch (Exception e) {
            AotAddon.LOGGER.warn("HookAttachLimitHandler reflection failure", e);
        }
    }

    /**
     * Advances one side's state machine by one tick and returns the updated tick count.
     * phaseSetter is how we write back the (possibly changed) Phase for that side, since Java
     * doesn't let us pass enum fields by reference.
     */
    private static int tickSide(Object hook, Field activeField, Field extendingField,
                                Field retractingField, Method releaseMethod,
                                Phase phase, int ticks, java.util.function.Consumer<Phase> phaseSetter)
            throws Exception {
        if (hook == null) {
            phaseSetter.accept(Phase.IDLE);
            return 0;
        }

        boolean active = activeField.getBoolean(hook);
        boolean extending = extendingField.getBoolean(hook);
        boolean retracting = retractingField.getBoolean(hook);
        boolean fullyLatched = active && !extending && !retracting;

        switch (phase) {
            case IDLE -> {
                if (fullyLatched) {
                    phaseSetter.accept(Phase.FILLING);
                    return 1;
                }
                return 0;
            }
            case FILLING -> {
                if (!fullyLatched) {
                    // Player released manually before hitting the limit — bar disappears.
                    phaseSetter.accept(Phase.IDLE);
                    return 0;
                }
                int next = ticks + 1;
                if (next >= LIMIT_TICKS) {
                    releaseMethod.invoke(hook);
                    phaseSetter.accept(Phase.COOLING_DOWN);
                    return 0;
                }
                return next;
            }
            case COOLING_DOWN -> {
                if (active) {
                    // Player tried to fire again mid-cooldown — cancel it immediately.
                    releaseMethod.invoke(hook);
                }
                int next = ticks + 1;
                if (next >= LIMIT_TICKS) {
                    playReadySound();
                    phaseSetter.accept(Phase.IDLE);
                    return 0;
                }
                return next;
            }
            default -> {
                return 0;
            }
        }
    }

    private static void playReadySound() {
        try {
            Class<?> soundManagerClass = Class.forName(DAOT_SOUND_MANAGER);
            Class<?> soundEventClass = Class.forName("net.minecraft.sounds.SoundEvent");
            Method playOneShot = soundManagerClass.getMethod(
                    "playLocalOneShot", soundEventClass, float.class, float.class);

            // Placeholder vanilla sound — swap for a custom ODM-specific cue once one exists.
            Holder<SoundEvent> sound = SoundEvents.NOTE_BLOCK_PLING;
            playOneShot.invoke(null, sound.value(), 1.0f, 1.0f);
        } catch (Exception e) {
            AotAddon.LOGGER.warn("HookAttachLimitHandler failed to play ready sound", e);
        }
    }
}