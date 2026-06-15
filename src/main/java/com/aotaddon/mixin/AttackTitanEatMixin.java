package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.util.WarhammerInheritanceTracker;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Hooks into AttackTitanEntity.tickEat() to detect two events:
 *
 * 1. The Attack Titan eats a Warhammer shifter player
 *    → grants Warhammer inheritance to the Attack Titan's rider
 *
 * 2. The Attack Titan's rider (as a player) is eaten by any titan
 *    → revokes Warhammer inheritance from that player
 *
 * Uses remap = false and string targets throughout to avoid
 * hard class references to Danny's mod.
 */
@Mixin(targets = "daot.AttackTitanEntity", remap = false)
public class AttackTitanEatMixin {

    /**
     * Injects at the moment the eat damage is dealt (tick 30 of tickEat).
     * At this point eatDamageDone is about to be set true and the grabbed
     * entity is still alive and accessible.
     *
     * We check: is the grabbed entity a ServerPlayer riding a WarhammerTitanEntity?
     * If yes → grant inheritance to the Attack Titan's rider.
     */
    @Inject(
        method = "tickEat()V",
        at = @At(
            value = "FIELD",
            target = "daot.AttackTitanEntity.eatDamageDone:Z",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD,
            ordinal = 0
        ),
        remap = false,
        cancellable = false
    )
    private void onEatDamageDealt(CallbackInfo ci) {
        try {
            // --- Get the grabbed entity ---
            int grabbedId = getGrabbedEntityId();
            if (grabbedId == -1) return;

            Object level = getLevel();
            if (level == null) return;

            // Skip on client side
            if (isClientSide(level)) return;

            Object grabbed = getEntityById(level, grabbedId);
            if (grabbed == null) return;

            // --- Check if grabbed entity is a player ---
            if (!(grabbed instanceof ServerPlayer grabbedPlayer)) return;

            // --- Check if grabbed player is riding a WarhammerTitanEntity ---
            Object vehicle = getVehicle(grabbed);
            if (vehicle == null) return;

            boolean isWarhammer = isWarhammerTitan(vehicle);
            if (!isWarhammer) return;

            // --- Get the Attack Titan's rider (the one doing the eating) ---
            UUID shifterUUID = getShifterUUID();
            if (shifterUUID == null) return;

            Object rider = getRiderByUUID(level, shifterUUID);
            if (!(rider instanceof ServerPlayer attackRider)) return;

            // --- Grant inheritance ---
            WarhammerInheritanceTracker.grantInheritance(attackRider);

            // Notify the rider
            attackRider.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    "You have inherited the power of the Warhammer Titan!"
                ).withStyle(net.minecraft.ChatFormatting.GOLD),
                false
            );

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] AttackTitanEatMixin.onEatDamageDealt failed: {}", e.getMessage());
        }
    }

    /**
     * Injects when the Attack Titan's OWN rider is being eaten by another titan.
     * We hook into the moment the Attack Titan's rider is grabbed as a passenger
     * by checking if the grabbed entity is our own shifter UUID.
     *
     * This handles the revoke case: if the Attack Titan player gets eaten,
     * they lose their Warhammer inheritance.
     */
    @Inject(
        method = "tickEat()V",
        at = @At(
            value = "FIELD",
            target = "daot.AttackTitanEntity.eatTicks:I",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD,
            ordinal = 0
        ),
        remap = false,
        cancellable = false
    )
    private void onEatTickIncrement(CallbackInfo ci) {
        try {
            // Only check at tick 30 (when damage is dealt) to avoid spam
            int eatTicks = getEatTicks();
            if (eatTicks != 30) return;

            int grabbedId = getGrabbedEntityId();
            if (grabbedId == -1) return;

            Object level = getLevel();
            if (level == null || isClientSide(level)) return;

            Object grabbed = getEntityById(level, grabbedId);
            if (!(grabbed instanceof ServerPlayer grabbedPlayer)) return;

            // If the grabbed player has Warhammer inheritance, revoke it
            // (they are being eaten — they lose the power)
            if (WarhammerInheritanceTracker.hasInheritance(grabbedPlayer)) {
                WarhammerInheritanceTracker.revokeInheritance(grabbedPlayer);
                grabbedPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "Your Warhammer inheritance has been lost!"
                    ).withStyle(net.minecraft.ChatFormatting.RED),
                    false
                );
            }

        } catch (Exception e) {
            AotAddon.LOGGER.error("[AotAddon] AttackTitanEatMixin.onEatTickIncrement failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // REFLECTION HELPERS — all access to Danny's classes goes through here
    // =========================================================================

    private int getGrabbedEntityId() throws Exception {
        Method m = this.getClass().getMethod("getGrabbedEntityId");
        return (int) m.invoke(this);
    }

    private int getEatTicks() throws Exception {
        Field f = this.getClass().getDeclaredField("eatTicks");
        f.setAccessible(true);
        return (int) f.get(this);
    }

    private Object getLevel() throws Exception {
        // method_37908() = level()
        Method m = this.getClass().getMethod("method_37908");
        return m.invoke(this);
    }

    private boolean isClientSide(Object level) throws Exception {
        // method_8608() = isClientSide()
        Method m = level.getClass().getMethod("method_8608");
        return (boolean) m.invoke(level);
    }

    private Object getEntityById(Object level, int id) throws Exception {
        // method_8469(int) = getEntity(int)
        Method m = level.getClass().getMethod("method_8469", int.class);
        return m.invoke(level, id);
    }

    private Object getVehicle(Object entity) throws Exception {
        // method_5854() = getVehicle()
        Method m = entity.getClass().getMethod("method_5854");
        return m.invoke(entity);
    }

    private boolean isWarhammerTitan(Object entity) {
        try {
            Class<?> whClass = Class.forName("daot.WarhammerTitanEntity");
            return whClass.isInstance(entity);
        } catch (Exception e) {
            return false;
        }
    }

    private UUID getShifterUUID() throws Exception {
        Method m = this.getClass().getMethod("getShifterUUID");
        Object result = m.invoke(this);
        if (result instanceof Optional<?> opt) {
            return opt.isPresent() ? (UUID) opt.get() : null;
        }
        return result instanceof UUID u ? u : null;
    }

    private Object getRiderByUUID(Object level, UUID uuid) throws Exception {
        // method_8469 by UUID doesn't exist — use server player list
        Method getServerMethod = level.getClass().getMethod("getServer");
        Object server = getServerMethod.invoke(level);
        Method getPlayerListMethod = server.getClass().getMethod("getPlayerList");
        Object playerList = getPlayerListMethod.invoke(server);
        Method getPlayerMethod = playerList.getClass().getMethod("getPlayer", UUID.class);
        return getPlayerMethod.invoke(playerList, uuid);
    }
}
