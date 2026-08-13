package com.aotaddon.gascanister;

import com.aotaddon.AotAddon;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Points GeckoLib at the three resource files for this block:
 *   - geometry: assets/titanreqiuem/geo/gas_block.geo.json  (your Blockbench export)
 *   - texture:  assets/titanreqiuem/textures/block/gas_block.png
 *   - animation: assets/titanreqiuem/animations/gas_block.animation.json
 *
 * The animation file can just contain an empty animation list for now if you
 * have no clips yet - GeckoLib still expects the resource to exist.
 */
public class GasCanisterGeoModel extends GeoModel<GasCanisterBlockEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "geo/gas_block.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/item/gas_block.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "animations/gas_block.animation.json");

    @Override
    public ResourceLocation getModelResource(GasCanisterBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GasCanisterBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GasCanisterBlockEntity animatable) {
        return ANIMATION;
    }
}