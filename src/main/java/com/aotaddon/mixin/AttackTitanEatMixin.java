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
 *    -> grants Warhammer inheritance to the Attack Titan's rider
 *
 * 2. A player with Warhammer inheritance is eaten by the Attack Titan
 *    -> revokes their inheritance
 *
 * Uses TAIL injection (safest) to avoid opcode-based injection issues.
 */
@Mixin(targets = "daot.AttackTitanEntity", remap = false)
public class AttackTitanEatMixin {

    /**
     * Injects at the TAIL of tickEat() every tick.
     * We check eatTicks == 30 internally (the damage tick) to only
     * run our logic once per eat sequence.
     */
    @Inject(
        method = "tickEat()V",
        at = @At("TAIL"),
        remap = false
    )
    private void onTickEatTail(CallbackInfo ci) {
        try {
            // Only act at tick 30 (when eat damage is dealt)
            int eatTicks = getEatTicks();
            if (eatTicks != 30) return;

            // Skip on client side
            Object level = getLevel();
            if (level == null || isClientSide(level)) return;

            int grabbedId = getGrabbedEntityId();
            if (grabbedId == -1) return;

            Object grabbed = getEntityById(level, grabbedId);
            if (grabbed == null) return;

            // Only care about player entities
            if (!(grabbed instanceof ServerPlayer grabbedPlayer)) return;

            // --- Case 1: grabbed player is riding a Warhammer Titan ---
            // Grant inheritance to the Attack Titan's rider
            Object vehicle = getVehicle(grabbed);
            if (vehicle != null && isWarhammerTitan(vehicle)) {
                UUID shifterUUID = getShifterUUID();
                if (shifterUUID != null) {
                    ServerPlayer attackRider = getRiderByUUID(level, shifterUUID);
                    if (attackRider != null) {
                        WarhammerInheritanceTracker.grantInheritance(attackRider);
                        attackRider.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                "You have inherited the power of the Warhammer Titan!"
                            ).withStyle(net.minecraft.ChatFormatting.GOLD),
                            false
                        );
                    }
                }
            }

            // --- Case 2: grabbed player has Warhammer inheritance ---
            // Revoke it - they are being eaten
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
            AotAddon.LOGGER.error("[AotAddon] AttackTitanEatMixin.onTickEatTail failed: {}", e.getMessage());
        }
    }

    // =========================================================================
    // REFLECTION HELPERS
    // =========================================================================

    private int getGrabbedEntityId() throws Exception {
        Method m = this.getClass().getMethod("getGrabbedEntityId");
        return (int) m.invoke(this);
    }

    private int getEatTicks() {
        try {
            Field f = this.getClass().getDeclaredField("eatTicks");
            f.setAccessible(true);
            return (int) f.get(this);
        } catch (Exception e) {
            // eatTicks field may be in a superclass - walk up
            try {
                Class<?> cls = this.getClass().getSuperclass();
                while (cls != null) {
                    try {
                        Field f = cls.getDeclaredField("eatTicks");
                        f.setAccessible(true);
                        return (int) f.get(this);
                    } catch (NoSuchFieldException ignored) {
                        cls = cls.getSuperclass();
                    }
                }
            } catch (Exception ignored) {}
            return -1;
        }
    }

    private Object getLevel() throws Exception {
        Method m = this.getClass().getMethod("method_37908");
        return m.invoke(this);
    }

    private boolean isClientSide(Object level) throws Exception {
        Method m = level.getClass().getMethod("method_8608");
        return (boolean) m.invoke(level);
    }

    private Object getEntityById(Object level, int id) throws Exception {
        Method m = level.getClass().getMethod("method_8469", int.class);
        return m.invoke(level, id);
    }

    private Object getVehicle(Object entity) throws Exception {
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

    private ServerPlayer getRiderByUUID(Object level, UUID uuid) throws Exception {
        Method getServerMethod = level.getClass().getMethod("getServer");
        Object server = getServerMethod.invoke(level);
        Method getPlayerListMethod = server.getClass().getMethod("getPlayerList");
        Object playerList = getPlayerListMethod.invoke(server);
        Method getPlayerMethod = playerList.getClass().getMethod("getPlayer", UUID.class);
        Object player = getPlayerMethod.invoke(playerList, uuid);
        return player instanceof ServerPlayer sp ? sp : null;
    }
}
