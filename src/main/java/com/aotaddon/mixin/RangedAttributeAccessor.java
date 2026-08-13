package com.aotaddon.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes RangedAttribute's private min/max fields so we can widen them
 * after the vanilla Attributes class has already constructed MAX_HEALTH
 * with its hardcoded 1024.0 ceiling.
 *
 * @Mutable is required because both fields are declared `final` — without
 * it Mixin generates a setter that the JVM rejects with IllegalAccessError
 * at verification time.
 */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {

    @Mutable
    @Accessor("minValue")
    void setMinValue(double min);

    @Mutable
    @Accessor("maxValue")
    void setMaxValue(double max);
}