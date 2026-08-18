package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only registration for SeveredPartEntity's renderer, same pattern
 * as GasCanisterClientSetup's onRegisterRenderers, but for an entity
 * renderer (registerEntityRenderer) rather than a block-entity renderer.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SeveredPartClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEVERED_PART.get(), SeveredPartRenderer::new);
        event.registerEntityRenderer(ModEntities.CAMPFIRE_SEAT.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
    }
}