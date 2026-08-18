package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.util.SpearHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixins into ModNetworking to replace handleThunderSpearLoad
 * with our bloodline-aware stacking version.
 *
 * Uses targets = string class name and remap = false throughout
 * so Danny's mod recompiles don't hard crash us.
 */
@Mixin(targets = "daot.network.ModNetworking", remap = false)
public class ThunderSpearLoadMixin {

    /**
     * Injects at the HEAD of handleThunderSpearLoad and cancels
     * the original entirely, replacing it with our logic.
     *
     * Full behavior:
     * 1. Check if player is Marleyan — block with message if so
     * 2. Detect which hands hold blades
     * 3. If any spears are currently loaded — UNLOAD ALL, return count to inventory
     * 4. If no spears loaded — run balance function, load max possible, consume from inventory
     */
    @Inject(
        method = "handleThunderSpearLoad(Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void onHandleThunderSpearLoad(ServerPlayer player,
                                                  CallbackInfo ci) {
        try {
            // Always cancel original — we handle everything
            ci.cancel();

            // --- Step 1: Marleyan block ---
            try {
                if (SpearHelper.isMarleyBlocked(player)) {
                    SpearHelper.sendMarleyanBlock(player);
                    return;
                }
            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Marleyan check failed: {}", e.getMessage());
                // Continue with default behavior if bloodline check fails
            }

            // --- Step 2: Detect blade hands ---
            ItemStack mainHand;
            ItemStack offHand;
            boolean hasMain;
            boolean hasOff;

            try {
                mainHand = player.getMainHandItem();
                offHand = player.getOffhandItem();
                hasMain = SpearHelper.isBlade(mainHand);
                hasOff = SpearHelper.isBlade(offHand);
            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Hand detection failed: {}", e.getMessage());
                return;
            }

            if (!hasMain && !hasOff) return;

            // --- Step 3: Read current counts ---
            int mainCurrent;
            int offCurrent;

            try {
                mainCurrent = hasMain ? SpearHelper.getCount(mainHand) : 0;
                offCurrent  = hasOff  ? SpearHelper.getCount(offHand)  : 0;
            } catch (Exception e) {
                AotAddon.LOGGER.error("[AotAddon] Count read failed: {}", e.getMessage());
                return;
            }

            boolean anyLoaded = mainCurrent > 0 || offCurrent > 0;

            if (anyLoaded) {
                // --- UNLOAD: return all spears to inventory ---
                try {
                    int totalReturn = mainCurrent + offCurrent;

                    if (hasMain && mainCurrent > 0) {
                        SpearHelper.setCount(mainHand, 0);
                    }
                    if (hasOff && offCurrent > 0) {
                        SpearHelper.setCount(offHand, 0);
                    }

                    if (!player.isCreative()) {
                        SpearHelper.returnToInventory(player, totalReturn);
                    }

                    AotAddon.LOGGER.debug("[AotAddon] Unloaded {} spears for {}",
                        totalReturn, player.getName().getString());

                } catch (Exception e) {
                    AotAddon.LOGGER.error("[AotAddon] Unload failed: {}", e.getMessage());
                }

            } else {
                // --- LOAD: balance and fill up to cap ---
                try {
                    // Get bloodline cap
                    int totalCap = SpearHelper.getCapForPlayer(player);
                    if (totalCap <= 0) {
                        // Cap is 0 — blocked (Marleyan fallback or config set to 0)
                        SpearHelper.sendMarleyanBlock(player);
                        return;
                    }

                    // Per hand cap is always half of total, rounded down
                    int perHandCap = totalCap / 2;

                    // How many available in inventory
                    int available = player.isCreative()
                        ? totalCap
                        : SpearHelper.countInInventory(player);

                    if (available <= 0 && !player.isCreative()) {
                        // No spears in inventory — send message and return
                        player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                "No thunder spears in inventory"
                            ).withStyle(net.minecraft.ChatFormatting.RED),
                            true
                        );
                        return;
                    }

                    // Run balance function
                    int[] allocation = SpearHelper.balance(
                        available,
                        hasMain, hasOff,
                        perHandCap,
                        mainCurrent, offCurrent
                    );

                    int mainLoad = allocation[0];
                    int offLoad  = allocation[1];
                    int totalLoad = mainLoad + offLoad;

                    if (totalLoad <= 0) return;

                    // Apply counts
                    if (hasMain && mainLoad > 0) {
                        SpearHelper.setCount(mainHand, mainCurrent + mainLoad);
                    }
                    if (hasOff && offLoad > 0) {
                        SpearHelper.setCount(offHand, offCurrent + offLoad);
                    }

                    // Consume from inventory
                    if (!player.isCreative()) {
                        SpearHelper.consumeFromInventory(player, totalLoad);
                    }

                    AotAddon.LOGGER.debug("[AotAddon] Loaded {} spears for {} (main={}, off={})",
                        totalLoad, player.getName().getString(), mainLoad, offLoad);

                } catch (Exception e) {
                    AotAddon.LOGGER.error("[AotAddon] Load failed: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            // Outer catch — something went very wrong, log and do nothing
            AotAddon.LOGGER.error("[AotAddon] ThunderSpearLoadMixin outer catch: {}", e.getMessage());
        }
    }
}
