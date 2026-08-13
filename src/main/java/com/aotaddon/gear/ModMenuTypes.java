package com.aotaddon.gear;

import com.aotaddon.AotAddon;
import com.aotaddon.access.PlayerInventoryAccessMenu;
import com.aotaddon.gascanister.GasCanisterBlockEntity;
import com.aotaddon.gascanister.GasCanisterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AotAddon.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<GearPouchMenu>> GEAR_POUCH =
            MENUS.register("gear_pouch",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, data) -> new GearPouchMenu(windowId, inv)
                    ));

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerInventoryAccessMenu>> PLAYER_INVENTORY_ACCESS =
            MENUS.register("player_inventory_access",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, buf) -> new PlayerInventoryAccessMenu(windowId, inv, buf.readVarInt())
                    ));

    public static final DeferredHolder<MenuType<?>, MenuType<GasCanisterMenu>> GAS_CANISTER =
            MENUS.register("gas_canister",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, buf) -> {
                                var pos = buf.readBlockPos();
                                if (inv.player.level().getBlockEntity(pos) instanceof GasCanisterBlockEntity be) {
                                    return new GasCanisterMenu(windowId, inv, be, be.getContainerData());
                                }
                                // Fallback shouldn't normally happen - block was removed/unloaded
                                // between the click and the packet arriving.
                                throw new IllegalStateException("No GasCanisterBlockEntity at " + pos);
                            }
                    ));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}