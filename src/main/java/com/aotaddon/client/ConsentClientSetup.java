package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.aotaddon.access.PlayerInventoryAccessScreen;
import com.aotaddon.access.ToggleConsentC2SPacket;
import com.aotaddon.gear.ModMenuTypes;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Mirrors GearPouchClientSetup exactly - registers the keybind + screen here,
 * actual per-tick consumeClick() detection lives in ODMDashClientSetup like
 * every other keybind in this project (see tickConsent() there).
 */
@EventBusSubscriber(modid = AotAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ConsentClientSetup {

    public static final KeyMapping TOGGLE_CONSENT = new KeyMapping(
            "key.titanreqiuem.toggle_consent",
            GLFW.GLFW_KEY_UNKNOWN, // unbound by default, player sets it in Controls
            "key.categories.titanreqiuem"
    );

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.PLAYER_INVENTORY_ACCESS.get(), PlayerInventoryAccessScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_CONSENT);
    }

    public static void onConsentKeyPressed() {
        PacketDistributor.sendToServer(new ToggleConsentC2SPacket());
    }
}