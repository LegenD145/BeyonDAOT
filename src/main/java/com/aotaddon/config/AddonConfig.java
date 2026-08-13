package com.aotaddon.config;
// am i retarded????
import net.neoforged.neoforge.common.ModConfigSpec;

public class AddonConfig {

    public static final ModConfigSpec SPEC;

    // Thunder Spear stacking
    public static final ModConfigSpec.IntValue DEFAULT_CAP;
    public static final ModConfigSpec.IntValue ELDIAN_CAP;
    public static final ModConfigSpec.IntValue ACKERMAN_CAP;
    public static final ModConfigSpec.BooleanValue MARLEY_BLOCKED;

    // Attribute caps
    public static final ModConfigSpec.DoubleValue MAX_HEALTH_CAP;

    // ODM Skill Tree XP
    public static final ModConfigSpec.DoubleValue ODM_XP_PER_GAS_UNIT;

    // ODM Skills — Impulse (double-W) and Dodge (double-S)
    public static final ModConfigSpec.BooleanValue ODM_SKILLS_ENABLED;
    public static final ModConfigSpec.IntValue ODM_SKILL_WINDOW_MS;

    public static final ModConfigSpec.IntValue ODM_IMPULSE_GAS_COST;
    public static final ModConfigSpec.IntValue ODM_IMPULSE_COOLDOWN_MS;
    public static final ModConfigSpec.DoubleValue ODM_IMPULSE_STRENGTH;

    public static final ModConfigSpec.IntValue ODM_DODGE_GAS_COST;
    public static final ModConfigSpec.IntValue ODM_DODGE_COOLDOWN_MS;
    public static final ModConfigSpec.DoubleValue ODM_DODGE_STRENGTH;
    public static final ModConfigSpec.IntValue ODM_DODGE_IFRAME_MS;

    public static final ModConfigSpec.IntValue ODM_TRAIL_DURATION_TICKS;

    // Female Titan decapitation (Impulse + full-charge eye slice)
    public static final ModConfigSpec.IntValue IMPULSE_DECAP_WINDOW_MS;

    // ODM Wall Climb
    public static final ModConfigSpec.BooleanValue ODM_WALL_CLIMB_ENABLED;
    public static final ModConfigSpec.DoubleValue ODM_WALL_CLIMB_SPEED;
    public static final ModConfigSpec.DoubleValue ODM_WALL_DESCEND_SPEED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // ------------------------------------------------------------------
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

        // ------------------------------------------------------------------
        builder.comment(
                "ODM Skills — Impulse (double-tap W) dashes in your look direction.",
                "Dodge (double-tap S) always dashes backward and grants brief damage immunity."
        ).push("odm_skills");

        ODM_SKILLS_ENABLED = builder
                .comment("Master toggle for Impulse and Dodge")
                .define("enabled", true);

        ODM_SKILL_WINDOW_MS = builder
                .comment("Milliseconds within which a second tap counts as a double-tap. Default 250ms.")
                .defineInRange("window_ms", 250, 50, 1000);

        // --- Impulse ---
        ODM_IMPULSE_GAS_COST = builder
                .comment("Gas consumed per Impulse dash.")
                .defineInRange("impulse_gas_cost", 12, 0, 500);

        ODM_IMPULSE_COOLDOWN_MS = builder
                .comment("Cooldown between Impulse uses. Default 6000ms (6s).")
                .defineInRange("impulse_cooldown_ms", 6000, 50, 10000);

        ODM_IMPULSE_STRENGTH = builder
                .comment("Impulse dash strength, applied along the full look direction. Default 1.3.")
                .defineInRange("impulse_strength", 1.3, 0.1, 10.0);

        // --- Dodge ---
        ODM_DODGE_GAS_COST = builder
                .comment("Gas consumed per Dodge. Kept cheap since it's a reactive panic button.")
                .defineInRange("dodge_gas_cost", 4, 0, 500);

        ODM_DODGE_COOLDOWN_MS = builder
                .comment("Cooldown between Dodge uses. Default 6000ms (6s).")
                .defineInRange("dodge_cooldown_ms", 6000, 50, 10000);

        ODM_DODGE_STRENGTH = builder
                .comment("Dodge backward dash strength. Default 1.1.")
                .defineInRange("dodge_strength", 1.1, 0.1, 10.0);

        ODM_DODGE_IFRAME_MS = builder
                .comment("Duration of damage immunity granted by Dodge, in ms. Default 800ms (matches Freedom War).")
                .defineInRange("dodge_iframe_ms", 800, 0, 5000);

        // --- Trail ---
        ODM_TRAIL_DURATION_TICKS = builder
                .comment("How many ticks the velocity-decay trail particle effect lasts after a skill fires.")
                .defineInRange("trail_duration_ticks", 7, 0, 40);

        // --- Decapitation ---
        IMPULSE_DECAP_WINDOW_MS = builder
                .comment("Window after Impulse fires during which a full-charge eye slice on Female Titan counts as a decapitation. Default 600ms.")
                .defineInRange("impulse_decap_window_ms", 600, 50, 5000);

        builder.pop();

        // ------------------------------------------------------------------
        builder.comment("ODM wall-climb settings (W = climb up, S = descend, no gas cost)").push("odm_wall_climb");

        ODM_WALL_CLIMB_ENABLED = builder
                .comment("Master toggle for the ODM wall-climb feature")
                .define("enabled", true);

        ODM_WALL_CLIMB_SPEED = builder
                .comment("Upward climb speed while perched on a wall. Default 0.15.")
                .defineInRange("climb_speed", 0.15, 0.01, 2.0);

        ODM_WALL_DESCEND_SPEED = builder
                .comment("Downward descend speed while perched on a wall. Default 0.1.")
                .defineInRange("descend_speed", 0.1, 0.01, 2.0);

        builder.pop();

        // ------------------------------------------------------------------
        builder.comment("Attribute caps — raises vanilla's hardcoded limits").push("attribute_caps");

        MAX_HEALTH_CAP = builder
                .comment(
                        "Raises the vanilla max_health attribute ceiling past the default 1024.0.",
                        "Applied once at mod startup. Needed for titan health values above 1024."
                )
                .defineInRange("max_health_cap", 100000.0, 1024.0, 1.0E9);

        builder.pop();

        // ------------------------------------------------------------------
        builder.comment("ODM skill tree XP gain — fires per gas unit consumed").push("odm_xp");

        ODM_XP_PER_GAS_UNIT = builder
                .comment(
                        "XP granted to the ODM skill tree per unit of gas consumed.",
                        "Matches KubeJS's st_addXP('odm', amount) leveling curve exactly.",
                        "Default 0.15 — roughly 1 XP per ~7 gas spent."
                )
                .defineInRange("xp_per_gas_unit", 0.15, 0.0, 100.0);

        builder.pop();

        // ------------------------------------------------------------------
        SPEC = builder.build();
    }
}