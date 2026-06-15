package com.aotaddon;

import com.aotaddon.config.AddonConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("titanreqiuem")
public class AotAddon {

    public static final String MOD_ID = "titanreqiuem";
    public static final Logger LOGGER = LoggerFactory.getLogger("aotaddon");

    public AotAddon(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AddonConfig.SPEC, "aotaddon-server.toml");
        LOGGER.info("AoT Addon thunder spear system loaded.");
    }
}