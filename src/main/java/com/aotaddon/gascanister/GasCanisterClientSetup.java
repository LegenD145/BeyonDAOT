package com.aotaddon.gascanister;

import com.aotaddon.AotAddon;
import com.aotaddon.gear.ModMenuTypes;
import com.aotaddon.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only registration for the Gas Canister block, same pattern as
 * GearPouchClientSetup: screen registration via RegisterMenuScreensEvent,
 * plus the GeoBlockRenderer registration this block additionally needs
 * since it renders via GeckoLib rather than a static blockstate model.
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GasCanisterClientSetup {

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.GAS_CANISTER.get(), GasCanisterScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GAS_CANISTER.get(), context -> new GasCanisterRenderer());
    }
}
