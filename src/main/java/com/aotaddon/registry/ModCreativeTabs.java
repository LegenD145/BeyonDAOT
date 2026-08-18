package com.aotaddon.registry;

import com.aotaddon.AotAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AotAddon.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BEYOND_THE_WALLS =
            TABS.register("beyond_the_walls", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.titanreqiuem.beyond_the_walls"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.ROYAL_MEDAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (var item : ModItems.ITEMS.getEntries()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
