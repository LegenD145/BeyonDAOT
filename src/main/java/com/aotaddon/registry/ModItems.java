package com.aotaddon.registry;

import com.aotaddon.AotAddon;
import com.aotaddon.item.ShifterUnlockPotionItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AotAddon.MOD_ID);

    // Paradis medals
    public static final DeferredItem<Item> BRONZE_MEDAL =
            ITEMS.registerSimpleItem("bronze_medal");
    public static final DeferredItem<Item> SILVER_MEDAL =
            ITEMS.registerSimpleItem("silver_medal");
    public static final DeferredItem<Item> GOLD_MEDAL =
            ITEMS.registerSimpleItem("gold_medal");
    public static final DeferredItem<Item> ROYAL_MEDAL =
            ITEMS.registerSimpleItem("royal_medal");

    // Marley banknotes
    public static final DeferredItem<Item> MARLEY_BANKNOTE_1 =
            ITEMS.registerSimpleItem("marley_banknote_1");
    public static final DeferredItem<Item> MARLEY_BANKNOTE_5 =
            ITEMS.registerSimpleItem("marley_banknote_5");
    public static final DeferredItem<Item> MARLEY_BANKNOTE_10 =
            ITEMS.registerSimpleItem("marley_banknote_10");
    public static final DeferredItem<Item> MARLEY_BANKNOTE_20 =
            ITEMS.registerSimpleItem("marley_banknote_20");
    public static final DeferredItem<Item> MARLEY_BANKNOTE_50 =
            ITEMS.registerSimpleItem("marley_banknote_50");
    public static final DeferredItem<Item> MARLEY_BANKNOTE_100 =
            ITEMS.registerSimpleItem("marley_banknote_100");
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GAS_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("gas_block", ModBlocks.GAS_CANISTER_BLOCK);
    // Female shifter permanent-unlock potions.
    // All three reuse the Armor Potion's geo model shape (placeholder texture =
    // copy of the armor potion texture until custom art is painted).
    public static final DeferredItem<ShifterUnlockPotionItem> ZERO_HOUR_FORMULA =
            ITEMS.register("zero_hour_formula", () -> new ShifterUnlockPotionItem(
                    new Item.Properties().stacksTo(1),
                    "has_zero_hour",
                    "female",
                    "Zero Hour Formula Acquired",
                    "Grants the Colossal detonation to the Female shifter",
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "geo/zero_hour_formula.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/item/zero_hour_formula.png")
            ));

    public static final DeferredItem<ShifterUnlockPotionItem> RESILIENCE_COMPOUND =
            ITEMS.register("resilience_compound", () -> new ShifterUnlockPotionItem(
                    new Item.Properties().stacksTo(1),
                    "has_resilience",
                    "female",
                    "Resilience Compound Acquired",
                    "Grants +6000 max stamina to the Female shifter",
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "geo/resilience_compound.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/item/resilience_compound.png")
            ));

    public static final DeferredItem<ShifterUnlockPotionItem> RAPTOR_COMPOUND =
            ITEMS.register("raptor_compound", () -> new ShifterUnlockPotionItem(
                    new Item.Properties().stacksTo(1),
                    "has_raptor",
                    "female",
                    "Raptor Compound Acquired",
                    "Grants a sound-barrier sprint burst to the Female shifter",
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "geo/raptor_compound.geo.json"),
                    ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "textures/item/raptor_compound.png")
            ));

    /**
     * NOTE: ITEMS.register(modEventBus) was never being called anywhere in the
     * codebase before this change - none of the currency items (medals, banknotes)
     * were actually reaching the game registry. Call this from AotAddon's
     * constructor: ModItems.register(modEventBus);
     */
    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
