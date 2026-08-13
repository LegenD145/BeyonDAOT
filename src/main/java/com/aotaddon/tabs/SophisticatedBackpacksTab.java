package com.aotaddon.tabs;

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
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;

/**
 * Sends the same no-arg BackpackOpenPayload the mod's own default keybind uses (see
 * KeybindHandler line ~177). Server-side, BackpackOpenPayload#handlePayload's
 * findAndOpenFirstBackpack walks every registered PlayerInventoryHandler ("main", "offhand",
 * and — critically — "curios" when Curios + the mod's CuriosCompat are both present) and opens
 * whichever backpack it finds first. We deliberately do NOT hand-roll our own Curios slot lookup:
 * the mod already resolves the equipped-backpack case end to end.
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
        PacketDistributor.sendToServer((CustomPacketPayload) new BackpackOpenPayload());
    }

    @Override
    public boolean isCurrentlyActive(Class<? extends Screen> currentScreenClass) {
        return BackpackScreen.class.equals(currentScreenClass);
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
