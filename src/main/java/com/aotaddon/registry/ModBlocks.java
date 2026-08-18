package com.aotaddon.registry;

import com.aotaddon.AotAddon;
import com.aotaddon.gascanister.GasCanisterBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(AotAddon.MOD_ID);

    public static final DeferredBlock<GasCanisterBlock> GAS_CANISTER_BLOCK =
            BLOCKS.register("gas_block",
                    () -> new GasCanisterBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus);
    }
}