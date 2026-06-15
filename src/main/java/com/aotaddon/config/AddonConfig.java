package com.aotaddon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AddonConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue DEFAULT_CAP;
    public static final ModConfigSpec.IntValue ELDIAN_CAP;
    public static final ModConfigSpec.IntValue ACKERMAN_CAP;
    public static final ModConfigSpec.BooleanValue MARLEY_BLOCKED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Thunder Spear stacking settings").push("thunder_spear");

        DEFAULT_CAP = builder
                .comment("Default spear cap for bloodlines not listed below")
                .defineInRange("default_cap", 4, 0, 64);

        ELDIAN_CAP = builder
                .comment("Spear cap for Eldian bloodline")
                .defineInRange("eldian_cap", 6, 0, 64);

        ACKERMAN_CAP = builder
                .comment("Spear cap for Ackerman bloodline")
                .defineInRange("ackerman_cap", 8, 0, 64);

        MARLEY_BLOCKED = builder
                .comment("If true, Marleyans are completely blocked from loading thunder spears")
                .define("marley_blocked", true);

        builder.pop();

        SPEC = builder.build();
    }
}