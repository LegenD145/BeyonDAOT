package com.aotaddon.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import com.aotaddon.AotAddon;

/**
 * Same as ODMGearTooltipHideMixin, but for daot.ODMAPGItem, which has its own
 * separate appendHoverText override with the identical "Gas line added first" structure.
 */
@Mixin(targets = "daot.ODMAPGItem", remap = false)
public class ODMAPGTooltipHideMixin {

    @Redirect(
            method = "appendHoverText",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0)
    )
    private boolean aotaddon$skipGasTooltip(List<Component> tooltipComponents, Object element) {
        AotAddon.LOGGER.info("[aotaddon] ODMAPGTooltipHideMixin: redirect HIT, skipping Gas tooltip line");
        return true;
    }
}