package com.aotaddon.combat;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Points GeckoLib at daot's own Female Titan model/texture/animation files
 * so SeveredPartEntity can reuse them without needing a separate export.
 * Resource locations confirmed against daot's FemaleTitanModel.java
 * (mod id "dannys-aot", not this addon's own mod id).
 *
 * Animation resource is only here because GeckoLib's GeoModel requires one
 * to exist - SeveredPartEntity never registers any controllers, so the
 * animation file is never actually driven; every bone stays at its default
 * bind-pose transform from the geo file.
 */
public class SeveredPartGeoModel extends GeoModel<SeveredPartEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath("dannys-aot", "geo/femaletitan.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("dannys-aot", "textures/entity/femaletitan.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath("dannys-aot", "animations/femaletitan.animation.json");

    @Override
    public ResourceLocation getModelResource(SeveredPartEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SeveredPartEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SeveredPartEntity animatable) {
        return ANIMATION;
    }
}