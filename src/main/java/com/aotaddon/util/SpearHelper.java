package com.aotaddon.util;

import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.UUID;

public class SpearHelper {

    // -------------------------------------------------------------------------
    // NBT key we write — replaces the vanilla "ThunderSpear" boolean
    // -------------------------------------------------------------------------
    private static final String COUNT_KEY = "ThunderSpearCount";
    private static final String LEGACY_KEY = "ThunderSpear";

    // -------------------------------------------------------------------------
    // Marleyan block message
    // -------------------------------------------------------------------------
    private static final String MARLEY_MESSAGE =
            "Even with earth's technology you are still too stupid to use such destructive powers compared to your garbage artillery";

    // =========================================================================
    // COUNT READ / WRITE — all NBT access goes through these two methods
    // =========================================================================

    /**
     * Reads the thunder spear count from a blade stack's custom data.
     * Falls back to reading the legacy boolean if the int key is absent,
     * so existing stacks in the world migrate gracefully.
     */
    public static int getCount(ItemStack stack) {
        try {
            // Reflect into the stack's custom data component to get the NBT tag
            // We use getOrDefault approach via reflection to avoid hard class refs
            Class<?> customDataClass = Class.forName("net.minecraft.world.item.component.CustomData");
            Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents");

            Field customDataField = dataComponentsClass.getField("CUSTOM_DATA");
            Object customDataComponent = customDataField.get(null);

            Method getMethod = stack.getClass().getMethod("get", Class.forName("net.minecraft.core.component.DataComponentType"));
            Object customData = getMethod.invoke(stack, customDataComponent);

            if (customData == null) return 0;

            Method copyTagMethod = customDataClass.getMethod("copyTag");
            CompoundTag tag = (CompoundTag) copyTagMethod.invoke(customData);

            if (tag == null) return 0;

            // New int key takes priority
            if (tag.contains(COUNT_KEY)) {
                return Math.max(0, tag.getInt(COUNT_KEY));
            }
            // Legacy bool migration — if spear bool is true, treat as count 1
            if (tag.contains(LEGACY_KEY) && tag.getBoolean(LEGACY_KEY)) {
                return 1;
            }
            return 0;

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] getCount reflection failed: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Writes the thunder spear count to a blade stack's custom data.
     * Also clears the legacy boolean key to avoid confusion.
     */
    public static void setCount(ItemStack stack, int count) {
        try {
            int clamped = Math.max(0, count);

            Class<?> customDataClass = Class.forName("net.minecraft.world.item.component.CustomData");
            Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents");
            Class<?> dataComponentTypeClass = Class.forName("net.minecraft.core.component.DataComponentType");

            Field customDataField = dataComponentsClass.getField("CUSTOM_DATA");
            Object customDataComponent = customDataField.get(null);

            // Get existing tag or start fresh
            Method getMethod = stack.getClass().getMethod("get", dataComponentTypeClass);
            Object existingData = getMethod.invoke(stack, customDataComponent);

            CompoundTag tag;
            if (existingData != null) {
                Method copyTagMethod = customDataClass.getMethod("copyTag");
                tag = (CompoundTag) copyTagMethod.invoke(existingData);
            } else {
                tag = new CompoundTag();
            }

            // Write new int key, clear legacy bool
            tag.putInt(COUNT_KEY, clamped);
            tag.remove(LEGACY_KEY);

            // Write back via CustomData.of(tag)
            Method ofMethod = customDataClass.getMethod("of", CompoundTag.class);
            Object newCustomData = ofMethod.invoke(null, tag);

            Method setMethod = stack.getClass().getMethod("set", dataComponentTypeClass, Object.class);
            setMethod.invoke(stack, customDataComponent, newCustomData);

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] setCount reflection failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // BLOODLINE CAP LOOKUP
    // =========================================================================

    /**
     * Returns the total spear cap for this player based on their bloodline.
     * Returns -1 if bloodline lookup fails (treated as default cap by callers).
     */
    public static int getCapForPlayer(ServerPlayer player) {
        try {
            int skillBonus = SkillTreeGearHelper.getSpearCapacityBonus(player);

            // BloodlineData.get(server) then .getBloodline(uuid)
            Class<?> bloodlineDataClass = Class.forName("daot.BloodlineData");
            Class<?> bloodlineTypeClass = Class.forName("daot.BloodlineType");

            Method getDataMethod = bloodlineDataClass.getMethod("get",
                    Class.forName("net.minecraft.server.MinecraftServer"));
            Object bloodlineData = getDataMethod.invoke(null, player.getServer());

            Method getBloodlineMethod = bloodlineDataClass.getMethod("getBloodline", UUID.class);
            Object bloodlineType = getBloodlineMethod.invoke(bloodlineData, player.getUUID());

            if (bloodlineType == null) {
                return AddonConfig.DEFAULT_CAP.get() + skillBonus;
            }

            // Compare by enum name as string — safe against recompiles
            String name = ((Enum<?>) bloodlineType).name().toUpperCase();

            return switch (name) {
                case "ACKERMAN" -> AddonConfig.ACKERMAN_CAP.get() + skillBonus;
                case "ELDIAN"   -> AddonConfig.ELDIAN_CAP.get() + skillBonus;
                case "MARLEY", "MARLEYAN" -> AddonConfig.MARLEY_BLOCKED.get()
                        ? 0
                        : AddonConfig.DEFAULT_CAP.get() + skillBonus;
                default -> AddonConfig.DEFAULT_CAP.get() + skillBonus;
            };

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] getCapForPlayer reflection failed: {}", e.getMessage());
            return AddonConfig.DEFAULT_CAP.get() + SkillTreeGearHelper.getSpearCapacityBonus(player);
        }
    }

    /**
     * True when this player is Marleyan and thunder-spear loading is config-blocked.
     */
    public static boolean isMarleyBlocked(ServerPlayer player) {
        return AddonConfig.MARLEY_BLOCKED.get() && isMarleyan(player);
    }

    /**
     * Returns true if this player's bloodline is Marleyan.
     */
    public static boolean isMarleyan(ServerPlayer player) {
        try {
            Class<?> bloodlineDataClass = Class.forName("daot.BloodlineData");

            Method getDataMethod = bloodlineDataClass.getMethod("get",
                    Class.forName("net.minecraft.server.MinecraftServer"));
            Object bloodlineData = getDataMethod.invoke(null, player.getServer());

            Method getBloodlineMethod = bloodlineDataClass.getMethod("getBloodline", UUID.class);
            Object bloodlineType = getBloodlineMethod.invoke(bloodlineData, player.getUUID());

            if (bloodlineType == null) return false;

            String name = ((Enum<?>) bloodlineType).name().toUpperCase();
            return name.equals("MARLEY") || name.equals("MARLEYAN");

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] isMarleyan reflection failed: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // BALANCE FUNCTION
    // =========================================================================

    /**
     * Given total available spears and how many blades are in hands,
     * returns an int[2] where [0] = main hand allocation, [1] = off hand allocation.
     *
     * Rules:
     * - Split equally across blade count
     * - Main hand gets ceil on uneven splits
     * - Neither hand exceeds perHandCap
     * - If only one blade, all go to that hand
     *
     * @param available    how many spear items are in inventory (or cap if creative)
     * @param hasMain      true if main hand holds a blade
     * @param hasOff       true if off hand holds a blade
     * @param perHandCap   max spears per hand derived from total cap
     * @param mainCurrent  spears already loaded on main hand blade
     * @param offCurrent   spears already loaded on off hand blade
     */
    public static int[] balance(int available, boolean hasMain, boolean hasOff,
                                int perHandCap, int mainCurrent, int offCurrent) {
        // How many more can each hand accept
        int mainRoom = hasMain ? Math.max(0, perHandCap - mainCurrent) : 0;
        int offRoom  = hasOff  ? Math.max(0, perHandCap - offCurrent)  : 0;

        int totalRoom = mainRoom + offRoom;
        int toLoad = Math.min(available, totalRoom);

        if (toLoad <= 0) return new int[]{0, 0};

        // Only one hand has a blade — all go there
        if (hasMain && !hasOff) return new int[]{Math.min(toLoad, mainRoom), 0};
        if (!hasMain && hasOff) return new int[]{0, Math.min(toLoad, offRoom)};

        // Both hands have blades — split with main getting ceil
        int mainShare = (int) Math.ceil(toLoad / 2.0);
        int offShare  = toLoad - mainShare;

        // Clamp to room
        mainShare = Math.min(mainShare, mainRoom);
        offShare  = Math.min(offShare,  offRoom);

        // If main was clamped, give leftovers to off and vice versa
        int leftover = toLoad - mainShare - offShare;
        if (leftover > 0) {
            int offExtra = Math.min(leftover, offRoom - offShare);
            offShare += offExtra;
            leftover -= offExtra;
        }
        if (leftover > 0) {
            int mainExtra = Math.min(leftover, mainRoom - mainShare);
            mainShare += mainExtra;
        }

        return new int[]{mainShare, offShare};
    }

    // =========================================================================
    // INVENTORY HELPERS
    // =========================================================================

    /**
     * Counts ThunderSpearItem stacks in the player's inventory via reflection.
     */
    public static int countInInventory(ServerPlayer player) {
        try {
            Class<?> thunderSpearItemClass = Class.forName("daot.ThunderSpearItem");
            int count = 0;

            for (ItemStack stack : player.getInventory().items) {
                try {
                    if (thunderSpearItemClass.isInstance(stack.getItem())) {
                        count += stack.getCount();
                    }
                } catch (Exception inner) {
                    // skip this slot
                }
            }
            return count;

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] countInInventory reflection failed: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Consumes the given number of ThunderSpearItem from the player's inventory.
     */
    public static void consumeFromInventory(ServerPlayer player, int amount) {
        try {
            Class<?> thunderSpearItemClass = Class.forName("daot.ThunderSpearItem");
            int remaining = amount;

            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                try {
                    if (!thunderSpearItemClass.isInstance(stack.getItem())) continue;
                    int take = Math.min(stack.getCount(), remaining);
                    stack.shrink(take);
                    remaining -= take;
                } catch (Exception inner) {
                    // skip this slot
                }
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] consumeFromInventory reflection failed: {}", e.getMessage());
        }
    }

    /**
     * Gives the player the given number of ThunderSpearItem stacks.
     * Uses reflection to get the DannysAot.THUNDER_SPEAR item instance.
     */
    public static void returnToInventory(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        try {
            Class<?> dannysAotClass = Class.forName("daot.DannysAot");
            Field thunderSpearField = dannysAotClass.getField("THUNDER_SPEAR");
            Object thunderSpearItem = thunderSpearField.get(null);

            Class<?> itemClass = Class.forName("net.minecraft.world.item.Item");
            ItemStack returnStack = new ItemStack((net.minecraft.world.item.Item) thunderSpearItem, amount);

            if (!player.getInventory().add(returnStack)) {
                // Inventory full — drop at player's feet
                player.drop(returnStack, false);
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] returnToInventory reflection failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // BLADE CHECK
    // =========================================================================

    /**
     * Returns true if the given ItemStack is a BladeItem via reflection.
     */
    public static boolean isBlade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            Class<?> bladeItemClass = Class.forName("daot.BladeItem");
            return bladeItemClass.isInstance(stack.getItem());
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] isBlade reflection failed: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // MESSAGE HELPER
    // =========================================================================

    public static void sendMarleyanBlock(ServerPlayer player) {
        player.displayClientMessage(
                Component.literal(MARLEY_MESSAGE).withStyle(ChatFormatting.RED),
                true // true = action bar
        );
    }
}
