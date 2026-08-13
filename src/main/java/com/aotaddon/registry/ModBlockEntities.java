package com.aotaddon.registry;

import com.aotaddon.AotAddon;
import com.aotaddon.gascanister.GasCanisterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AotAddon.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasCanisterBlockEntity>> GAS_CANISTER =
            BLOCK_ENTITIES.register("gas_block",
                    () -> BlockEntityType.Builder.of(
                            GasCanisterBlockEntity::new,
                            ModBlocks.GAS_CANISTER_BLOCK.get()
                    ).build(null));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}