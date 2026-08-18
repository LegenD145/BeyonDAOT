package com.aotaddon.tabs;

import com.aotaddon.AotAddon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * FTB Teams integration tab. Uses reflection so ftbteams is optional at runtime.
 */
public class FtbTeamsTab extends TabBase {

    private static final String FTB_TEAMS_MOD_ID = "ftbteams";
    private static final ItemStack ICON = new ItemStack(Items.WHITE_BANNER);

    public static boolean isFtbTeamsLoaded() {
        return ModList.get().isLoaded(FTB_TEAMS_MOD_ID);
    }

    @Override
    public void registerOnScreens() {
        if (!isFtbTeamsLoaded()) {
            return;
        }
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> 176, p -> 166, 20);
    }

    @Override
    public boolean isEnabled(Player player) {
        return isFtbTeamsLoaded();
    }

    @Override
    public void onClick(Player player) {
        if (!isFtbTeamsLoaded()) {
            return;
        }
        try {
            Class<?> messageClass = Class.forName("dev.ftb.mods.ftbteams.net.OpenGUIMessage");
            Method sendToServer = messageClass.getMethod("sendToServer");
            sendToServer.invoke(null);
        } catch (Exception e) {
            AotAddon.LOGGER.warn("[AotAddon] Failed to open FTB Teams tab: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        if (!isFtbTeamsLoaded()) {
            return false;
        }
        try {
            Class<?> screenClass = Class.forName("dev.ftb.mods.ftbteams.client.gui.MyTeamScreen");
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
        return Component.literal("Team");
    }
}
