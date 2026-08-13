package com.aotaddon.mixin;
//if u can see this im going to leave
import com.aotaddon.AotAddon;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that guards all daot.* targeting mixins during mixin preparation.
 *
 * Uses MixinService.getService().getBytecodeProvider().getClassBytes() to check
 * class availability — reads raw bytecode without triggering static initializers,
 * which is required to avoid the "Not bootstrapped" crash under Sinytra Connector.
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

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!targetClassName.startsWith("daot.")) return true;

        try {
            org.objectweb.asm.tree.ClassNode node = MixinService.getService()
                    .getBytecodeProvider()
                    .getClassNode(targetClassName, true);
            return node != null;
        } catch (Exception e) {
            AotAddon.LOGGER.warn(
                    "[AotAddon] Skipping mixin {} — target {} not available: {}",
                    mixinClassName, targetClassName, e.getMessage()
            );
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {}
}