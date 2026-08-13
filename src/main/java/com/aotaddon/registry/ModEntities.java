package com.aotaddon.registry;

import com.aotaddon.AotAddon;
import com.aotaddon.combat.SeveredPartEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AotAddon.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SeveredPartEntity>> SEVERED_PART =
            ENTITY_TYPES.register("severed_part",
                    () -> EntityType.Builder.<SeveredPartEntity>of(SeveredPartEntity::new, MobCategory.MISC)
                            .sized(2.0f, 2.0f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("severed_part"));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
