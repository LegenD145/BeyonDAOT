package com.aotaddon.mixin;

import com.aotaddon.titan.RandomRetargetGoal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Targets daot.TitanEntity via string (remap = false) since it's not on the
 * compile classpath, same convention as the rest of the DAOT mixins.
 *
 * IMPORTANT: this class stub extends Mob purely so `this.targetSelector`,
 * `this.getType()`, etc. resolve at compile time - those are inherited
 * VANILLA fields/methods, not DAOT internals, so no reflection is needed
 * for them. Only the injection target itself (method_5959 = registerGoals,
 * as compiled in the original DAOT jar) needs the raw intermediary name.
 *
 * === WIRING NOTE ===
 * Add this class to your aotaddon mixins json (wherever your other DAOT
 * mixins - e.g. the Gear Pouch blade-reload one - are listed).
 */
@Mixin(targets = "daot.TitanEntity", remap = false)
public abstract class TitanEntityRandomRetargetMixin extends Mob {

    protected TitanEntityRandomRetargetMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "method_5959", at = @At("TAIL"), remap = false)
    private void aotaddon$replaceAbnormalRetargeting(CallbackInfo ci) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        if (id == null) {
            return;
        }

        String path = id.getPath();
        boolean isAbnormal = path.equals("abnormal_titan") || path.equals("crawling_abnormal_titan");
        if (!isAbnormal) {
            return;
        }

        // Strip out DAOT's built-in "always retarget to closest" goal so it
        // doesn't fight with ours.
        Set<WrappedGoal> toRemove = new HashSet<>();
        for (WrappedGoal wrapped : this.targetSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal.getClass().getSimpleName().equals("TitanFindTargetGoal")) {
                toRemove.add(wrapped);
            }
        }
        for (WrappedGoal wrapped : toRemove) {
            this.targetSelector.removeGoal(wrapped.getGoal());
        }

        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        this.targetSelector.addGoal(2, new RandomRetargetGoal(this, range, 0.06f));
    }
}