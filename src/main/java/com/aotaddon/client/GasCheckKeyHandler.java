package com.aotaddon.client;

import com.aotaddon.gas.DaotGasReflection;
import com.aotaddon.gas.GasTierText;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Client-only. Tap-to-check gas indicator, replacing daot's always-on gas bar.
 * No packets needed: the local player's own equipped ItemStack (with its NBT-stored
 * gas value) is already fully available on the client, so we can read it directly.
 *
 * Registration required in AotAddon's constructor:
 *   modEventBus.addListener(GasCheckKeyHandler::registerKeyMapping);
 *   NeoForge.EVENT_BUS.addListener(GasCheckKeyHandler::onClientTick);
 */
public final class GasCheckKeyHandler {

    public static final KeyMapping CHECK_GAS_KEY = new KeyMapping(
            "key.aotaddon.check_gas",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.aotaddon"
    );

    private GasCheckKeyHandler() {}

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(CHECK_GAS_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (CHECK_GAS_KEY.consumeClick()) {
            checkGas();
        }
    }

    private static void checkGas() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // ODM gear is registered as a LEGGINGS-slot armor piece in daot (confirmed via
        // ODM_GEAR's ArmorItem.Type constant in DannysAot's field init), despite daot's
        // own HUD code misleadingly naming its local variable "chestItem" - it's LEGS too.
        ItemStack legsItem = mc.player.getItemBySlot(EquipmentSlot.LEGS);
        if (legsItem.isEmpty() || !DaotGasReflection.isODMGear(legsItem.getItem())) {
            return;
        }

        int gas = DaotGasReflection.getGas(legsItem);
        int maxGas = DaotGasReflection.getMaxGas(legsItem);
        if (gas < 0 || maxGas <= 0) {
            return;
        }

        String text = GasTierText.resolve(gas, maxGas);
        if (text != null) {
            mc.gui.setOverlayMessage(Component.literal(text), false);
            GasCheckAnimation.play(mc.player);
        }
    }
}
