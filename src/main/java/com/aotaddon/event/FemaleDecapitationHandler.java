package com.aotaddon.event;

import com.aotaddon.combat.DecapitationHandler;
import com.aotaddon.combat.ShifterTitanHelper;
import com.aotaddon.combat.HitboxDeduper;
import com.aotaddon.util.ImpulseWindowHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * AOT Freedom War-style decapitation: if a player's blade lands a hit on
 * Female Titan's eye hitbox while daot's own charged-ODM deep-slice gate has
 * already been satisfied (that's what allows this event to fire on the eye
 * entity at all - see BladeAttackTracker's shallow-slice block), AND the hit
 * lands within this player's Impulse window, the normal eye-damage outcome
 * is replaced with an instant decapitation.
 *
 * Any DAOT shifter titan eye hitbox during Impulse window → decapitation.
 * shifter outright (see DecapitationHandler) - incapacitate-instead-of-kill
 * is a later step once the headless-but-alive render state exists.
 *
 * Uses LivingIncomingDamageEvent (cancelable, fires before damage
 * mitigation) rather than LivingDamageEvent.Post (already-applied, used
 * elsewhere in this mod for tracking/xp where cancellation isn't needed).
 *
 * NOT annotated with @SubscribeEvent - registered manually via
 * NeoForge.EVENT_BUS.addListener() in AotAddon's constructor. The event
 * parameter type is fully qualified inline rather than imported at the top
 * of the file, matching DodgeIFrameHandler's pattern exactly, so
 * LivingIncomingDamageEvent is never loaded at class-load time.
 */
public class FemaleDecapitationHandler {

    public static void onEyeHurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!ShifterTitanHelper.isTitanEyeEntity(target)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ImpulseWindowHandler.isWithinImpulseWindow(player.getUUID())) {
            return;
        }

        Entity parent = HitboxDeduper.resolveParentTitan(target);
        if (!(parent instanceof LivingEntity shifterTitan)) {
            return;
        }
        if (!ShifterTitanHelper.isShifterTitan(shifterTitan)) {
            return;
        }

        // This is a decapitation, not a normal eye hit - cancel the usual
        // outcome entirely (including whatever damage BladeAttackTracker
        // was about to apply to the eye entity itself).
        event.setCanceled(true);

        DecapitationHandler.decapitate(shifterTitan, player);
    }
}