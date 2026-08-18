package com.aotaddon.tabs;

import com.aotaddon.AotAddon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Constructor;

/**
 * Sophisticated Backpacks integration tab. Uses reflection so the mod is optional.
 */
public class SophisticatedBackpacksTab extends TabBase {

    private static final String SOPHISTICATED_BACKPACKS_MOD_ID = "sophisticatedbackpacks";
    private static final ItemStack ICON = new ItemStack(Items.LEATHER);

    public static boolean isSophisticatedBackpacksLoaded() {
        return ModList.get().isLoaded(SOPHISTICATED_BACKPACKS_MOD_ID);
    }

    @Override
    public void registerOnScreens() {
        if (!isSophisticatedBackpacksLoaded()) {
            return;
        }
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> 176, p -> 166, 40);
    }

    @Override
    public boolean isEnabled(Player player) {
        return isSophisticatedBackpacksLoaded();
    }

    @Override
    public void onClick(Player player) {
        if (!isSophisticatedBackpacksLoaded()) {
            return;
        }
        try {
            Class<?> payloadClass = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload");
            Constructor<?> ctor = payloadClass.getConstructor();
            CustomPacketPayload payload = (CustomPacketPayload) ctor.newInstance();
            PacketDistributor.sendToServer(payload);
        } catch (Exception e) {
            AotAddon.LOGGER.warn("[AotAddon] Failed to open Sophisticated Backpacks tab: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        if (!isSophisticatedBackpacksLoaded()) {
            return false;
        }
        try {
            Class<?> screenClass = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen");
            return screenClass.isAssignableFrom(currentScreenClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void renderIcon(GuiGraphics graphics, int x, int y, int width, int height) {
        int iconX = x + (width - 16) / 2;
        int iconY = y + (height - 16) / 2;
        graphics.renderItem(ICON, iconX, iconY);
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Backpack");
    }
}
