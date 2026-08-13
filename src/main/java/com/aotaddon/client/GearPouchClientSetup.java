package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.gear.GearPouchScreen;
import com.aotaddon.gear.ModMenuTypes;
import com.aotaddon.network.OpenGearPouchPayload;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GearPouchClientSetup {

    public static final KeyMapping KEY_GEAR_POUCH = new KeyMapping(
            "key.titanreqiuem.gear_pouch",
            GLFW.GLFW_KEY_G,
            "key.categories.titanreqiuem"
    );

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.GEAR_POUCH.get(), GearPouchScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_GEAR_POUCH);
    }

    public static void onGearPouchKeyPressed() {
        PacketDistributor.sendToServer(new OpenGearPouchPayload());
    }
}