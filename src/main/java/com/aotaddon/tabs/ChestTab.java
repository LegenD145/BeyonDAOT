package com.aotaddon.tabs;

import com.aotaddon.gear.GearPouchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The "home" tab — always first (priority 0). Represents plain player inventory. Registered on
 * every other tab-bar screen so clicking it always gets you back to InventoryScreen; registered
 * on InventoryScreen itself too so the tab row is consistent across all of them, even though
 * clicking it while already on InventoryScreen is a no-op.
 */
public class ChestTab extends TabBase {

    private static final ItemStack ICON = new ItemStack(Items.CHEST);

    @Override
    public void registerOnScreens() {
        TabsMenu.addTabToScreen(this, InventoryScreen.class, p -> 176, p -> 166, 0);
        TabsMenu.addTabToScreen(this, GearPouchScreen.class, p -> 176, p -> 120, 0);
        // Add further addTabToScreen(...) calls here as each new tab-bar screen
        // (Xaero's GuiMap, LSO BodyHealthScreen, etc.) comes online.
    }

    @Override
    public void onClick(Player player) {
        if (Minecraft.getInstance().screen instanceof InventoryScreen) {
            return; // already home, nothing to do
        }
        Minecraft.getInstance().setScreen(new InventoryScreen(player));
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return InventoryScreen.class.equals(currentScreenClass);
    }

    @Override
    public void renderIcon(GuiGraphics graphics, int x, int y, int width, int height) {
        // Center a real 16x16 item icon in whatever box TabButton hands us — no custom art needed.
        int iconX = x + (width - 16) / 2;
        int iconY = y + (height - 16) / 2;
        graphics.renderItem(ICON, iconX, iconY);
    }

    @Override
    public Component getTooltip() {
        return Component.literal("Inventory");
    }
}
