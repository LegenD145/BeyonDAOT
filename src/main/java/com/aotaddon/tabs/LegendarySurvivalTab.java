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
import sfiomn.legendarysurvivaloverhaul.client.screens.BodyHealthScreen;

/**
 * Opens Legendary Survival Overhaul's body-damage screen in view-only mode. BodyHealthScreen has
 * only one constructor (Player, InteractionHand, boolean, int, float, int) — passing hand=null
 * makes the screen's own init() disable every heal button while still populating body-part
 * damage from AttachmentUtil, so this is a real view-only mode already built into the mod, not
 * something we're faking.
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
        Minecraft.getInstance().setScreen(
                new BodyHealthScreen(player, null, false, 0, 0.0f, 0));
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return BodyHealthScreen.class.equals(currentScreenClass);
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
