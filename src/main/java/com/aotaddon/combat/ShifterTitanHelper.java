package com.aotaddon.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Map;

/** Shared checks and render metadata for DAOT shifter titans. */
public final class ShifterTitanHelper {

    private static final float MODEL_TO_BLOCK = 1f / 16f;

    private record TitanAssets(String geo, String texture, String animation, float headPivotY) {}

    private static final Map<String, TitanAssets> ASSETS = Map.ofEntries(
            Map.entry("FemaleTitanEntity", new TitanAssets("femaletitan", "femaletitan", "femaletitan", 205.87f)),
            Map.entry("ArmoredTitanEntity", new TitanAssets("armoredtitan", "armoredtitan", "armoredtitan", 205f)),
            Map.entry("AttackTitanEntity", new TitanAssets("attacktitan2", "attacktitan2", "attacktitan2", 205f)),
            Map.entry("BeastTitanEntity", new TitanAssets("beast_titan", "beasttitan", "beasttitan", 200f)),
            Map.entry("CartShifterTitanEntity", new TitanAssets("cart", "cart", "cart", 160f)),
            Map.entry("ColossalTitanEntity", new TitanAssets("colossal", "colossal", "colossal", 640f)),
            Map.entry("JawTitanEntity", new TitanAssets("jawtitan", "jawtitan", "jawtitan", 190f)),
            Map.entry("WarhammerTitanEntity", new TitanAssets("warhammertitan", "warhammertitan", "warhammertitan", 205f))
    );

    private ShifterTitanHelper() {}

    public static boolean isTitanEyeEntity(Entity entity) {
        return entity != null && entity.getClass().getSimpleName().endsWith("EyeEntity");
    }

    public static boolean isShifterTitan(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getSimpleName();
        if (!className.endsWith("TitanEntity")) {
            return false;
        }
        Package pkg = entity.getClass().getPackage();
        return pkg != null && pkg.getName().startsWith("daot");
    }

    public static String geoBaseName(String titanClassSimpleName) {
        return assetsFor(titanClassSimpleName).geo();
    }

    public static String textureBaseName(String titanClassSimpleName) {
        return assetsFor(titanClassSimpleName).texture();
    }

    public static String animationBaseName(String titanClassSimpleName) {
        return assetsFor(titanClassSimpleName).animation();
    }

    public static double headWorldOffset(LivingEntity titan) {
        if (titan == null) {
            return 1.8;
        }
        return headWorldOffset(titan.getClass().getSimpleName());
    }

    public static double headWorldOffset(String titanClassSimpleName) {
        return assetsFor(titanClassSimpleName).headPivotY() * MODEL_TO_BLOCK;
    }

    /** Y offset applied before drawing the severed head geo (Blockbench pixels → blocks). */
    public static double severedHeadRenderAnchor(String titanClassSimpleName) {
        return headWorldOffset(titanClassSimpleName);
    }

    private static TitanAssets assetsFor(String titanClassSimpleName) {
        if (titanClassSimpleName == null || titanClassSimpleName.isBlank()) {
            return ASSETS.get("FemaleTitanEntity");
        }
        TitanAssets mapped = ASSETS.get(titanClassSimpleName);
        if (mapped != null) {
            return mapped;
        }
        String fallback = titanClassSimpleName.replace("Entity", "").toLowerCase(Locale.ROOT);
        return new TitanAssets(fallback, fallback, fallback, 205f);
    }
}
