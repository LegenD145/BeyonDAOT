package com.aotaddon.client;

import com.aotaddon.util.ShifterTitanUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-side yaw override for shiftlock — this is the piece that actually fixes the
 * visual lock for the driving player. daot's own titan movement code runs a separate
 * client-side prediction of the titan's facing (see AttackTitanEntity.tickRidden's
 * isClientSide() branch) that recalculates yaw every client tick independent of
 * whatever the server does — so a server-only override never reaches what the driver
 * actually sees. This runs after that prediction each client tick and overwrites it.
 *
 * Called from ODMDashClientSetup's onClientTickEnd, same as the other tick handlers.
 */
public final class ShiftlockClientTick {

    private ShiftlockClientTick() {}

    public static void tick() {
        if (!ShiftlockClientState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null || !ShifterTitanUtil.isShifterTitan(vehicle)) return;

        LivingEntity titan = (LivingEntity) vehicle;
        float camYaw = mc.player.getYRot();

        vehicle.setYRot(camYaw);
        vehicle.yRotO = camYaw;
        titan.yBodyRot = camYaw;
        titan.yBodyRotO = camYaw;
        titan.yHeadRot = camYaw;
        titan.yHeadRotO = camYaw;
    }
}