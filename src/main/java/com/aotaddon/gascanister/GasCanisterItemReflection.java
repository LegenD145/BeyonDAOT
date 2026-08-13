package com.aotaddon.gas;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Reflection bridge into daot's portable GasCanisterItem (the item you carry,
 * not the block we're building). Confirmed via decompile: static getGas(ItemStack)
 * and setGas(ItemStack, int) with a hard-coded max of 500.
 */
public final class GasCanisterItemReflection {

    private static final String CANISTER_CLASS = "daot.GasCanisterItem";
    public static final int CANISTER_MAX_GAS = 500;

    private static Class<?> canisterClass;
    private static Method getGasMethod;
    private static Method setGasMethod;
    private static boolean initFailed = false;

    private GasCanisterItemReflection() {}

    private static void init() {
        if (canisterClass != null || initFailed) return;
        try {
            canisterClass = Class.forName(CANISTER_CLASS);
            getGasMethod = canisterClass.getMethod("getGas", ItemStack.class);
            setGasMethod = canisterClass.getMethod("setGas", ItemStack.class, int.class);
        } catch (Exception e) {
            initFailed = true;
            System.err.println("[aotaddon] Failed to bind to daot.GasCanisterItem: " + e);
        }
    }

    public static boolean isGasCanisterItem(Item item) {
        init();
        if (initFailed || item == null) return false;
        return canisterClass.isInstance(item);
    }

    public static int getGas(ItemStack stack) {
        init();
        if (initFailed || stack == null || stack.isEmpty()) return -1;
        try {
            return (int) getGasMethod.invoke(null, stack);
        } catch (Exception e) {
            return -1;
        }
    }

    public static void setGas(ItemStack stack, int amount) {
        init();
        if (initFailed || stack == null || stack.isEmpty()) return;
        try {
            setGasMethod.invoke(null, stack, amount);
        } catch (Exception e) {
            System.err.println("[aotaddon] Failed to set canister gas: " + e);
        }
    }
}