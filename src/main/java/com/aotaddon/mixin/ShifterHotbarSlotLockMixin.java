package com.aotaddon.mixin;

import com.aotaddon.util.ShifterTitanUtil;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks every hotbar-relevant packet from a player while they're riding a
 * shifter titan body:
 *  - ServerboundSetCarriedItemPacket - switching slots (scroll/number keys)
 *  - ServerboundUseItemPacket - right-click "use in air" (eating, drinking,
 *    drawing a bow, most gun mods that implement Item#use() with a cooldown)
 *  - ServerboundUseItemOnPacket - right-click on a block
 *  - ServerboundSwingPacket - left-click swing (melee attack on air/block)
 *  - ServerboundInteractPacket - left/right click on an entity (attack or
 *    interact)
 *
 * The first version of this only cancelled the high-level NeoForge events
 * (PlayerInteractEvent, LivingEntityUseItemEvent, AttackEntityEvent) - those
 * didn't stop actual item use, because some items (including apparently
 * whatever's being tested here) don't fire those hooks reliably, or fire
 * them too late to matter. This intercepts the raw client packets instead,
 * the same way the slot-lock already does - nothing gets a chance to run
 * server-side at all.
 *
 * If items are STILL usable after this, the item in question almost
 * certainly isn't using any of the five vanilla packets above at all - e.g.
 * a gun mod that fires through its own custom NeoForge network payload
 * rather than piggybacking on vanilla use/attack packets. That would need a
 * mixin (or a compat hook) into that specific mod's packet handler instead;
 * there's no generic way to intercept an arbitrary mod's own packet type
 * without knowing what it is.
 *
 * ServerGamePacketListenerImpl is vanilla and on the compile classpath (this
 * isn't a daot target), so this is a normal remapped mixin - no reflection,
 * no remap=false, no Class.forName. `player` is inherited from
 * ServerCommonPacketListenerImpl; Mixin resolves the @Shadow up the real
 * hierarchy without needing an explicit extends clause here.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ShifterHotbarSlotLockMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
    private void aotaddon$blockCarriedItem(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (aotaddon$isRidingShifter()) ci.cancel();
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void aotaddon$blockUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (aotaddon$isRidingShifter()) ci.cancel();
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void aotaddon$blockUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (aotaddon$isRidingShifter()) ci.cancel();
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void aotaddon$blockSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (aotaddon$isRidingShifter()) ci.cancel();
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void aotaddon$blockInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (aotaddon$isRidingShifter()) ci.cancel();
    }

    private boolean aotaddon$isRidingShifter() {
        Entity vehicle = this.player.getVehicle();
        return vehicle != null && ShifterTitanUtil.isShifterTitan(vehicle);
    }
}