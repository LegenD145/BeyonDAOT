package com.aotaddon.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import com.aotaddon.AotAddon;

/**
 * Strips the "Gas: X/500" tooltip line from ODM gear's hover text while keeping
 * the other two lines (blade/APG equip reminder, boost key reminder).
 *
 * Targets the first list.add(...) call inside appendHoverText via ordinal - the Gas
 * line is always added first in both daot.ODMGearItem and daot.ODMAPGItem.
 *
 * NOTE: method name assumed as "appendHoverText" (the Mojmap name for the vanilla
 * Item override this replaces) rather than the raw intermediary "method_7851" seen
 * in the decompile - confirm against how an existing Danny-mixin targets an overridden
 * vanilla method (e.g. BladeItemMixin) and swap the method name here if it differs.
 */
@Mixin(targets = "daot.ODMGearItem", remap = false)
public class ODMGearTooltipHideMixin {

    @Redirect(
            method = "appendHoverText",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0)
    )
    private boolean aotaddon$skipGasTooltip(List<Component> tooltipComponents, Object element) {
        AotAddon.LOGGER.info("[aotaddon] ODMGearTooltipHideMixin: redirect HIT, skipping Gas tooltip line");
        // Deliberately don't add the Gas line. Return true to mimic a successful add.
        return true;
    }
}