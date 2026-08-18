package com.aotaddon.tabs;

import com.aotaddon.AotAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Opens Xaero's fullscreen world map. All Xaero API access is via reflection so
 * this class loads even when xaeroworldmap is not installed.
 */
public class XaerosMapTab extends TabBase {

    private static final String XAERO_MOD_ID = "xaeroworldmap";
    private static final ItemStack ICON = new ItemStack(Items.MAP);

    public static boolean isXaeroLoaded() {
        return ModList.get().isLoaded(XAERO_MOD_ID);
    }

    @Override
    public void registerOnScreens() {
        if (!isXaeroLoaded()) {
            return;
        }
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> 176, p -> 166, 10);
    }

    @Override
    public boolean isEnabled(Player player) {
        return isXaeroLoaded();
    }

    @Override
    public void onClick(Player player) {
        if (!isXaeroLoaded()) {
            return;
        }
        try {
            Class<?> sessionClass = Class.forName("xaero.map.WorldMapSession");
            Method getCurrentSession = sessionClass.getMethod("getCurrentSession");
            Object session = getCurrentSession.invoke(null);
            if (session == null) {
                return;
            }

            Method getMapProcessor = sessionClass.getMethod("getMapProcessor");
            Object mapProcessor = getMapProcessor.invoke(session);

            Screen current = Minecraft.getInstance().screen;
            Object camera = Minecraft.getInstance().getCameraEntity();

            Class<?> guiMapClass = Class.forName("xaero.map.gui.GuiMap");
            Class<?> mapProcessorClass = Class.forName("xaero.map.MapProcessor");
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            Constructor<?> ctor = guiMapClass.getConstructor(
                    Screen.class, guiMapClass, mapProcessorClass, entityClass);
            Screen mapScreen = (Screen) ctor.newInstance(current, null, mapProcessor, camera);
            Minecraft.getInstance().setScreen(mapScreen);
        } catch (Exception e) {
            AotAddon.LOGGER.warn("[AotAddon] Failed to open Xaero map tab: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        if (!isXaeroLoaded()) {
            return false;
        }
        try {
            Class<?> guiMapClass = Class.forName("xaero.map.gui.GuiMap");
            return guiMapClass.isAssignableFrom(currentScreenClass);
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
        return Component.literal("Map");
    }
}
