package com.aotaddon.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * One shared GeoModel for all ShifterUnlockPotionItem instances - each item carries
 * its own model/texture ResourceLocation (set at construction), this class just
 * resolves them. No animation resource since these are static-pose items, same as
 * DAOT's own ArmorPotionItemModel.
 */
public class ShifterUnlockPotionModel extends GeoModel<ShifterUnlockPotionItem> {

    @Override
    public ResourceLocation getModelResource(ShifterUnlockPotionItem animatable) {
        return animatable.getModelResource();
    }

    @Override
    public ResourceLocation getTextureResource(ShifterUnlockPotionItem animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(ShifterUnlockPotionItem animatable) {
        return null;
    }
}
