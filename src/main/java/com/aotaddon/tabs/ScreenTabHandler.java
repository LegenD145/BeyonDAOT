package com.aotaddon.tabs;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Bridges ScreenEvent to TabsMenu. Registered manually on NeoForge.EVENT_BUS (client-only) from
 * AotAddon's constructor, same pattern as HelosHudRenderer.
 *
 * Only screens that are AbstractContainerScreen get their tab row's position corrected every
 * frame from the screen's real panel origin (getGuiLeft/getGuiTop). Plain Screen targets (Xaero's
 * GuiMap, LSO's BodyHealthScreen) have no such origin, so their tabs stay at the centered guess
 * TabsMenu computes from the registered width/height at init time — good enough since those
 * screens don't resize/reposition their content after opening.
 */
public class ScreenTabHandler {

    public void onScreenInit(ScreenEvent.Init.Post event) {
        TabsMenu.initScreenButtons(event.getScreen(), event::addListener);
    }

    public void onScreenRender(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            TabsMenu.updateButtonsPosition(screen, containerScreen.getGuiLeft(), containerScreen.getGuiTop());
        }
    }
}
