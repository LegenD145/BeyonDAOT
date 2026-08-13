package com.aotaddon.tabs;

import dev.ftb.mods.ftbteams.client.gui.MyTeamScreen;
import dev.ftb.mods.ftbteams.net.OpenGUIMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

/**
 * FTB Teams doesn't let us construct its screen directly — MyTeamScreen needs
 * TeamPropertyCollection + PlayerPermissions that only exist after a server round trip. Instead
 * we fire the same OpenGUIMessage.sendToServer() the mod's own keybind uses (see
 * FTBTeamsClient.keyPressed) and let FTB Teams handle the response and screen swap itself.
 *
 * This means there's a brief delay between clicking this tab and the screen actually changing —
 * not instant like ChestTab/XaerosMapTab.
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
        OpenGUIMessage.sendToServer();
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return MyTeamScreen.class.equals(currentScreenClass);
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
