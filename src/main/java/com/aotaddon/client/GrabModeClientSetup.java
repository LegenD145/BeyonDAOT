package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.carry.ToggleGrabModeC2SPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GrabModeClientSetup {

    public static final KeyMapping TOGGLE_GRAB_MODE = new KeyMapping(
            "key.titanreqiuem.toggle_grab_mode",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.titanreqiuem"
    );

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_GRAB_MODE);
    }

    public static void onGrabModeKeyPressed() {
        PacketDistributor.sendToServer(new ToggleGrabModeC2SPacket());
    }
}