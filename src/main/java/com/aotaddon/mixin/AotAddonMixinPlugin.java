package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that gracefully skips any mixin whose target class
 * (daot.*) is not available on the classpath at apply time.
 *
 * This handles the Sinytra Connector scenario where Danny's AoT
 * (a Fabric mod) may not be visible to NeoForge mixins during
 * early loading. Instead of crashing, we simply skip the mixin
 * and log a warning.
 */
public class AotAddonMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        AotAddon.LOGGER.info("[AotAddon] Mixin plugin loaded for package: {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Called before each mixin is applied.
     * Returns false to skip the mixin if the target class is not found.
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only guard mixins that target daot.* classes
        if (!targetClassName.startsWith("daot.")) return true;

        try {
            Class.forName(targetClassName, false, this.getClass().getClassLoader());
            return true; // Target found - apply the mixin
        } catch (ClassNotFoundException e) {
            AotAddon.LOGGER.warn(
                "[AotAddon] Skipping mixin {} - target class {} not found. " +
                "Danny's AoT may not be loaded yet or is unavailable.",
                mixinClassName, targetClassName
            );
            return false; // Skip gracefully instead of crashing
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {}
}
