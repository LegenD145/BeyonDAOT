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

/**
 * Legendary Survival Overhaul body screen tab. Uses reflection so LSO is optional.
 */
public class LegendarySurvivalTab extends TabBase {

    private static final String LSO_MOD_ID = "legendarysurvivaloverhaul";
    private static final ItemStack ICON = new ItemStack(Items.GOLDEN_APPLE);

    public static boolean isLsoLoaded() {
        return ModList.get().isLoaded(LSO_MOD_ID);
    }

    @Override
    public void registerOnScreens() {
        if (!isLsoLoaded()) {
            return;
        }
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> 176, p -> 166, 30);
    }

    @Override
    public boolean isEnabled(Player player) {
        return isLsoLoaded();
    }

    @Override
    public void onClick(Player player) {
        if (!isLsoLoaded()) {
            return;
        }
        try {
            Class<?> screenClass = Class.forName(
                    "sfiomn.legendarysurvivaloverhaul.client.screens.BodyHealthScreen");
            Constructor<?> ctor = screenClass.getConstructor(
                    Player.class,
                    net.minecraft.world.InteractionHand.class,
                    boolean.class,
                    int.class,
                    float.class,
                    int.class);
            Screen bodyScreen = (Screen) ctor.newInstance(player, null, false, 0, 0.0f, 0);
            Minecraft.getInstance().setScreen(bodyScreen);
        } catch (Exception e) {
            AotAddon.LOGGER.warn("[AotAddon] Failed to open LSO body tab: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        if (!isLsoLoaded()) {
            return false;
        }
        try {
            Class<?> screenClass = Class.forName(
                    "sfiomn.legendarysurvivaloverhaul.client.screens.BodyHealthScreen");
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
        return Component.literal("Body");
    }
}
