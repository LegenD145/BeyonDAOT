package com.aotaddon.combat;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Reuses the parent shifter titan's DAOT geo/texture for severed head rendering.
 */
public class SeveredPartGeoModel extends GeoModel<SeveredPartEntity> {

    private static final String MOD = "dannys-aot";

    @Override
    public ResourceLocation getModelResource(SeveredPartEntity animatable) {
        String base = ShifterTitanHelper.geoBaseName(animatable.getTitanClassName());
        return ResourceLocation.fromNamespaceAndPath(MOD, "geo/" + base + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SeveredPartEntity animatable) {
        String base = ShifterTitanHelper.textureBaseName(animatable.getTitanClassName());
        return ResourceLocation.fromNamespaceAndPath(MOD, "textures/entity/" + base + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(SeveredPartEntity animatable) {
        String base = ShifterTitanHelper.animationBaseName(animatable.getTitanClassName());
        return ResourceLocation.fromNamespaceAndPath(MOD, "animations/" + base + ".animation.json");
    }
}
