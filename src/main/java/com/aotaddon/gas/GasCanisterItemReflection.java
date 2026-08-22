package com.aotaddon.gas;

import com.aotaddon.AotAddon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.lang.reflect.Method;

/**
 * Reflection bridge into daot's portable GasCanisterItem (the item you carry,
 * not the large gas block). Falls back to registry id + common NBT keys when
 * the daot class cannot be loaded at runtime.
 */
public final class GasCanisterItemReflection {

    private static final String CANISTER_CLASS = "daot.GasCanisterItem";
    public static final int CANISTER_MAX_GAS = 500;

    private static final String[] GAS_NBT_KEYS = {"Gas", "gas", "StoredGas", "stored_gas", "GasAmount"};

    private static Class<?> canisterClass;
    private static Method getGasMethod;
    private static Method setGasMethod;
    private static boolean reflectionBound;
    private static boolean reflectionFailed;

    private GasCanisterItemReflection() {}

    private static void bindReflection() {
        if (reflectionBound || reflectionFailed) {
            return;
        }
        try {
            canisterClass = Class.forName(CANISTER_CLASS);
            getGasMethod = canisterClass.getMethod("getGas", ItemStack.class);
            setGasMethod = canisterClass.getMethod("setGas", ItemStack.class, int.class);
            reflectionBound = true;
        } catch (Exception e) {
            reflectionFailed = true;
            AotAddon.LOGGER.warn("[AotAddon] daot.GasCanisterItem reflection unavailable, using registry/NBT fallback");
        }
    }

    public static boolean isGasCanisterItem(Item item) {
        if (item == null) {
            return false;
        }

        bindReflection();
        if (reflectionBound && canisterClass.isInstance(item)) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }

        String path = id.getPath().toLowerCase();
        if (path.contains("gas_canister") || path.contains("gascanister") || path.contains("gas_tank")) {
            return true;
        }

        return ("daot".equals(id.getNamespace()) || "dannys-aot".equals(id.getNamespace()))
                && path.contains("gas") && !path.contains("gas_block");
    }

    public static int getGas(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }

        bindReflection();
        if (reflectionBound) {
            try {
                return (int) getGasMethod.invoke(null, stack);
            } catch (Exception ignored) {
            }
        }

        return readGasFromNbt(stack);
    }

    public static void setGas(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        int clamped = Math.max(0, Math.min(amount, CANISTER_MAX_GAS));

        bindReflection();
        if (reflectionBound) {
            try {
                setGasMethod.invoke(null, stack, clamped);
                return;
            } catch (Exception ignored) {
            }
        }

        writeGasToNbt(stack, clamped);
    }

    private static int readGasFromNbt(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return 0;
        }
        CompoundTag tag = custom.copyTag();
        for (String key : GAS_NBT_KEYS) {
            if (tag.contains(key)) {
                return tag.getInt(key);
            }
        }
        return 0;
    }

    private static void writeGasToNbt(ItemStack stack, int amount) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = custom != null ? custom.copyTag() : new CompoundTag();
        tag.putInt("Gas", amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
