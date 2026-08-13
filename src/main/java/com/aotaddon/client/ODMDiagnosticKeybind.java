package com.aotaddon.client;
// ZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOS
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Keybind definitions for the AoT Addon.
 * Registration happens in AotAddon via modEventBus.addListener(RegisterKeyMappingsEvent).
 */
public class ODMDiagnosticKeybind {

    public static final KeyMapping KEY_DIAGNOSE = new KeyMapping(
            "key.titanreqiuem.odm_diagnose",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.titanreqiuem"
    );

    public static final KeyMapping KEY_BASTION = new KeyMapping(
            "key.titanreqiuem.bastion_toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.titanreqiuem"
    );

    public static final KeyMapping KEY_HORSE_WHISTLE = new KeyMapping(
            "key.titanreqiuem.horse_whistle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.titanreqiuem"
    );

    public static final KeyMapping KEY_SHIFTLOCK = new KeyMapping(
            "key.titanreqiuem.shiftlock_toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            "key.categories.titanreqiuem"
    );
}