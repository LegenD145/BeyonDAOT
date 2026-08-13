package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.item.ShifterUnlockPotionItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

/**
 * Wires the GeckoLib renderer to Zero Hour Formula / Resilience Compound / Raptor
 * Compound. Same lazy-consumer pattern DAOT itself uses for ArmorPotionItem in
 * DannysAotClient - GeckoLib calls createGeoRenderer() early, we hand it a deferred
 * supplier here once client resources are actually available.
 *
 * NOTE: mirrors DAOT's exact GeoRenderProvider construction shape from the
 * decompile. Verify against your GeckoLib jar version if this doesn't compile -
 * the constructor signature for GeoRenderProvider wasn't fully visible in the
 * decompiled source.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShifterUnlockPotionClientSetup {

    private static ShifterUnlockPotionItemRenderer sharedRenderer;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // clientRendererConsumer is a single static field shared by every
            // ShifterUnlockPotionItem instance (same as DAOT's own per-class
            // static field pattern) - one assignment covers all three potions,
            // since the shared model already resolves geo/texture per-instance.
            ShifterUnlockPotionItem.clientRendererConsumer = consumer -> consumer.accept(new GeoRenderProvider() {
                @Override
                public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                    if (sharedRenderer == null) {
                        sharedRenderer = new ShifterUnlockPotionItemRenderer();
                    }
                    return sharedRenderer;
                }
            });
            AotAddon.LOGGER.info("[AotAddon] Shifter unlock potion renderer wired.");
        });
    }
}
