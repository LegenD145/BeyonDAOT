package com.aotaddon.gas;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Reflection bridge into daot's DannysAot gas API.
 * daot is not a compile-time dependency, so every call goes through reflection.
 * Class/Method lookups are cached once (not per-call) to keep this cheap enough
 * to call every time the gas-check key is pressed.
 */
public final class DaotGasReflection {

    private static final String DANNYS_AOT_CLASS = "daot.DannysAot";

    private static Class<?> dannysAotClass;
    private static Method isODMGearMethod;
    private static Method getMaxGasForGearMethod;
    private static Method getGasFromGearMethod;
    private static Method setGasOnGearMethod;
    private static boolean initFailed = false;

    private DaotGasReflection() {}

    private static void init() {
        if (dannysAotClass != null || initFailed) {
            return;
        }
        try {
            dannysAotClass = Class.forName(DANNYS_AOT_CLASS);
            isODMGearMethod = dannysAotClass.getMethod("isODMGear", Item.class);
            getMaxGasForGearMethod = dannysAotClass.getMethod("getMaxGasForGear", ItemStack.class);
            getGasFromGearMethod = dannysAotClass.getMethod("getGasFromGear", ItemStack.class);
            setGasOnGearMethod = dannysAotClass.getMethod("setGasOnGear", ItemStack.class, int.class);
        } catch (Exception e) {
            initFailed = true;
            System.err.println("[aotaddon] Failed to bind to daot gas API via reflection: " + e);
        }
    }

    /**
     * Sets the gas value directly on the gear's ItemStack (confirmed to exist via
     * GasCanisterItem's own right-click-to-fill-gear logic in the daot decompile).
     * Caller is responsible for clamping to getMaxGas(stack) beforehand.
     */
    public static void setGas(ItemStack stack, int amount) {
        init();
        if (initFailed || stack == null || stack.isEmpty()) {
            return;
        }
        try {
            setGasOnGearMethod.invoke(null, stack, amount);
        } catch (Exception e) {
            System.err.println("[aotaddon] Failed to set gas on gear: " + e);
        }
    }

    /**
     * @return true if the given item is any daot ODM gear item (standard gear or APG).
     */
    public static boolean isODMGear(Item item) {
        init();
        if (initFailed || item == null) {
            return false;
        }
        try {
            return (boolean) isODMGearMethod.invoke(null, item);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return the current gas value stored on the stack, or -1 if unavailable.
     */
    public static int getGas(ItemStack stack) {
        init();
        if (initFailed || stack == null || stack.isEmpty()) {
            return -1;
        }
        try {
            return (int) getGasFromGearMethod.invoke(null, stack);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * @return the max gas capacity for the stack (500 for standard gear, 1000 for APG),
     * or -1 if unavailable.
     */
    public static int getMaxGas(ItemStack stack) {
        init();
        if (initFailed || stack == null || stack.isEmpty()) {
            return -1;
        }
        try {
            return (int) getMaxGasForGearMethod.invoke(null, stack);
        } catch (Exception e) {
            return -1;
        }
    }
}