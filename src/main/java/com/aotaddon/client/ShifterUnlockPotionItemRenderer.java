package com.aotaddon.client;

import com.aotaddon.item.ShifterUnlockPotionItem;
import com.aotaddon.item.ShifterUnlockPotionModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib item renderer for ShifterUnlockPotionItem, mirrors DAOT's own
 * ArmorPotionItemRenderer shape (a plain GeoItemRenderer<T> subclass constructed
 * with the shared model). One renderer instance is reused for all three potions
 * since the model class already resolves per-item geo/texture.
 */
public class ShifterUnlockPotionItemRenderer extends GeoItemRenderer<ShifterUnlockPotionItem> {

    public ShifterUnlockPotionItemRenderer() {
        super(new ShifterUnlockPotionModel());
    }
}
