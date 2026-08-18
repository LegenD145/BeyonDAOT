package com.aotaddon.client.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Keybind for opening the player's book menu (Stats / Talents / Settings /
 * Reputation / [unassigned]).
 *
 * K is already taken by ODMDiagnosticKeybind.KEY_DIAGNOSE, so this uses M.
 *
 * Registration required in AotAddon's constructor, same pattern as
 * GasCheckKeyHandler:
 *   modEventBus.addListener(BookKeybind::registerKeyMapping);
 *   NeoForge.EVENT_BUS.addListener(BookKeybind::onClientTick);
 */
public final class BookKeybind {

    public static final KeyMapping OPEN_BOOK_KEY = new KeyMapping(
            "key.titanreqiuem.open_book",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            "key.categories.titanreqiuem"
    );

    private BookKeybind() {}

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BOOK_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_BOOK_KEY.consumeClick()) {
            openBook();
        }
    }

    private static void openBook() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.screen != null) {
            return;
        }
        mc.setScreen(new BookCoverScreen());
    }
}
