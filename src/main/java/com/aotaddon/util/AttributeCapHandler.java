package com.aotaddon.util;

import com.aotaddon.AotAddon;
import com.aotaddon.config.AddonConfig;
import com.aotaddon.mixin.RangedAttributeAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Widens the max_health attribute's hard ceiling past vanilla's 1024.0 cap.
 *
 * Vanilla registers minecraft:generic.max_health as a RangedAttribute with
 * min=1.0, max=1024.0. Any entity's max health attribute silently clamps to
 * that range no matter what value a mod tries to set — including Danny's
 * ModConfig titan health values.
 *
 * This widens the ceiling using RangedAttributeAccessor (an @Accessor mixin
 * into RangedAttribute) so titan health can be set arbitrarily high.
 *
 * Called once from AotAddon's constructor — safe to run at mod startup since
 * Attributes is a pure vanilla class with no Sinytra Connector involvement.
 */
public class AttributeCapHandler {

    public static void applyCap() {
        double newCap;
        try {
            newCap = AddonConfig.MAX_HEALTH_CAP.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet (this runs at mod construction, before
            // world load) — use the hardcoded default directly instead.
            newCap = 100000.0;
        }

        try {
            var holder = Attributes.MAX_HEALTH;
            RangedAttribute attribute = (RangedAttribute) holder.value();

            ((RangedAttributeAccessor) attribute).setMaxValue(newCap);

            AotAddon.LOGGER.info("[AttributeCap] minecraft:generic.max_health ceiling raised to {}", newCap);
        } catch (Exception e) {
            AotAddon.LOGGER.error("[AttributeCap] Failed to widen max_health cap: {}", e.toString());
        }
    }
}
