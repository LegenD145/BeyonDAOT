package com.aotaddon.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

/**
 * Opens Xaero's fullscreen world map. Confirmed against the decompiled xaero source:
 * WorldMapSession.getCurrentSession().getMapProcessor() is the real pattern the mod itself uses
 * (see SupportXaeroWorldmap.getWorldMapScreenForOption), and getCurrentSession() can return null
 * if Xaero hasn't finished initializing yet — guarded below.
 *
 * Registration on other tab screens (LSO, FTB Teams, Sophisticated Backpacks, GearPouchScreen)
 * happens once each of those tabs exists too — see registerOnScreens.
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
        // Add TabsMenu.addTabToScreen(...) for GearPouchScreen / LSO / FTB Teams / Sophisticated
        // Backpacks screens as each comes online, same as ChestTab does.
    }

    @Override
    public boolean isEnabled(Player player) {
        return isXaeroLoaded();
    }

    @Override
    public void onClick(Player player) {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            // Xaero's map isn't initialized yet (e.g. very early after joining) — nothing to open.
            return;
        }
        Screen current = Minecraft.getInstance().screen;
        GuiMap mapScreen = new GuiMap(current, null, session.getMapProcessor(),
                Minecraft.getInstance().getCameraEntity());
        Minecraft.getInstance().setScreen(mapScreen);
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return GuiMap.class.equals(currentScreenClass);
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
