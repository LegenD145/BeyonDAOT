package com.aotaddon.carry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Grab mode works the same way consent mode does: mode ON is the consent
 * itself, no separate accept/deny step. Both players enabled + empty-hand
 * right-click = immediate carry.
 */
public final class CarryManager {

    private static final String PERSIST_KEY = "aotaddon_grab_mode";

    // UUIDs of carried players whose CURRENT dismount attempt is authorized
    // (the carrier just released them via shift). Anything not in this set
    // gets its dismount cancelled - see CarryInteractionEvents.
    private static final Set<UUID> EXPECTED_RELEASE = new HashSet<>();

    private CarryManager() {
    }

    public static InteractionResult onPlayerInteract(Player player, InteractionHand hand, Entity entity, HitResult hitResult) {
        if (!(entity instanceof Player target)) {
            return InteractionResult.PASS;
        }
        if (!player.getItemInHand(hand).isEmpty() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (hitResult != null) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.PASS;
        }
        // Don't let an already-carried player initiate a carry, and don't let
        // someone already carrying someone start a second one.
        if (player.isPassenger() || target.isPassenger() || !player.getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!isEnabled(player) || !isEnabled(target)) {
            player.sendSystemMessage(Component.literal("Both players must have grab mode enabled.").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        boolean success = target.startRiding(player, false);
        if (success) {
            target.sendSystemMessage(Component.literal("You are now being carried by " + player.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.literal("You are now carrying " + target.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean isEnabled(Player player) {
        return player.getPersistentData().getBoolean(PERSIST_KEY);
    }

    public static boolean toggle(Player player) {
        boolean enabled = !isEnabled(player);
        setEnabled(player, enabled);
        return enabled;
    }

    public static void setEnabled(Player player, boolean enabled) {
        player.getPersistentData().putBoolean(PERSIST_KEY, enabled);
    }

    public static boolean isCarrying(Player carrier, Player carried) {
        return carried.getVehicle() == carrier;
    }

    public static boolean isBeingCarried(Player player) {
        return player.getVehicle() instanceof Player;
    }

    public static boolean isCarryingAnyone(Player carrier) {
        for (Entity passenger : carrier.getPassengers()) {
            if (passenger instanceof Player) {
                return true;
            }
        }
        return false;
    }

    /**
     * Carrier-initiated release (shift key). This is the ONLY sanctioned way
     * for a carry to end - the carried player has no dismount option of their
     * own (enforced in CarryInteractionEvents via EntityMountEvent).
     */
    public static void releaseCarry(Player carrier) {
        for (Entity passenger : new java.util.ArrayList<>(carrier.getPassengers())) {
            if (!(passenger instanceof Player carried)) {
                continue;
            }
            EXPECTED_RELEASE.add(carried.getUUID());
            carried.stopRiding();
            EXPECTED_RELEASE.remove(carried.getUUID());

            carried.sendSystemMessage(Component.literal("You were set down by " + carrier.getName().getString() + ".").withStyle(ChatFormatting.YELLOW));
            carrier.sendSystemMessage(Component.literal("You set down " + carried.getName().getString() + ".").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static boolean isReleaseExpected(UUID carriedPlayerUuid) {
        return EXPECTED_RELEASE.contains(carriedPlayerUuid);
    }
}