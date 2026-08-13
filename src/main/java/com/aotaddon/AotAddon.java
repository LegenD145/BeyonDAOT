package com.aotaddon;

import com.aotaddon.access.ToggleConsentC2SPacket;
import com.aotaddon.carry.ToggleGrabModeC2SPacket;
import com.aotaddon.client.GasCheckKeyHandler;
import com.aotaddon.client.HelosHudRenderer;
import com.aotaddon.client.ODMDiagnosticKeybind;
import com.aotaddon.config.AddonConfig;
import com.aotaddon.currency.BalanceCommand;
import com.aotaddon.family.FamilyEventHandler;
import com.aotaddon.family.SetFamilyCommand;
import com.aotaddon.campfire.CampfireRegenHandler;
import com.aotaddon.identity.ChatBroadcastSuppressor;
import com.aotaddon.identity.IdentityNametagHandler;
import com.aotaddon.identity.IdentityRevealHandler;
import com.aotaddon.gear.ModMenuTypes;
import com.aotaddon.horse.HorseWhistleHandler;
import com.aotaddon.network.OpenGearPouchPayload;
import com.aotaddon.network.BastionTogglePayload;
import com.aotaddon.network.ShiftlockTogglePayload;
import com.aotaddon.network.ShiftlockStateSyncPayload;
import com.aotaddon.network.DodgeStartPayload;
import com.aotaddon.network.HorseWhistlePayload;
import com.aotaddon.network.HeadlessTitanSyncPayload;
import com.aotaddon.network.OdmGasXpPayload;
import com.aotaddon.network.SkillEffectPayload;
import com.aotaddon.network.TrailEffectPayload;
import com.aotaddon.network.RewardPopupPayload;
import com.aotaddon.network.HonorSyncPayload;
import com.aotaddon.network.CurrencySyncPayload;
import com.aotaddon.network.RevealIdentityPayload;
import com.aotaddon.network.IdentityFullSyncPayload;
import com.aotaddon.rewards.CurrencySyncTicker;
import com.aotaddon.rewards.TitanKillRewardHandler;
import com.aotaddon.client.RewardPopupOverlay;
import com.aotaddon.client.CurrencyHudOverlay;
import com.aotaddon.client.PdSkullOverlay;
import com.aotaddon.client.CustomHeartOverlay;
import com.aotaddon.registry.ModAttachments;
import com.aotaddon.registry.ModBlocks;
import com.aotaddon.registry.ModBlockEntities;
import com.aotaddon.registry.ModItems;
import com.aotaddon.tabs.ChestTab;
import com.aotaddon.tabs.FtbTeamsTab;
import com.aotaddon.tabs.LegendarySurvivalTab;
import com.aotaddon.tabs.PlaceholderTab;
import com.aotaddon.tabs.ScreenTabHandler;
import com.aotaddon.tabs.SophisticatedBackpacksTab;
import com.aotaddon.tabs.TabsMenu;
import com.aotaddon.tabs.XaerosMapTab;
import com.aotaddon.util.AttributeCapHandler;
import com.aotaddon.util.CombatTagHandler;
import com.aotaddon.util.DodgeIFrameHandler;
import com.aotaddon.util.RaptorDashHandler;
import com.aotaddon.util.ResilienceStaminaHandler;
import com.aotaddon.util.ZeroHourExplosionHandler;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("titanreqiuem")
public class AotAddon {

    public static final String MOD_ID = "titanreqiuem";
    public static final Logger LOGGER = LoggerFactory.getLogger("aotaddon");

    public AotAddon(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AddonConfig.SPEC, "aotaddon-server.toml");

        // Attachments
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Gas canister block + its BlockEntity
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Items (medals, gas canister block item)
        ModItems.ITEMS.register(modEventBus);

        // Severed body part entity (decapitation, limb loss)
        com.aotaddon.registry.ModEntities.register(modEventBus);

        // Widen max_health attribute ceiling past vanilla's 1024.0 cap
        AttributeCapHandler.applyCap();

        // Menu types
        ModMenuTypes.register(modEventBus);

        // Network payloads
        modEventBus.addListener(AotAddon::registerPayloads);

        // Keybinds
        modEventBus.addListener(AotAddon::registerKeyMappings);

        // Commands
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                SetFamilyCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                BalanceCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) ->
                com.aotaddon.command.PurCommand.register(e.getDispatcher()));

        // Family events
        NeoForge.EVENT_BUS.addListener(FamilyEventHandler::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(FamilyEventHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(FamilyEventHandler::onPlayerFirstJoin);
        NeoForge.EVENT_BUS.addListener(FamilyEventHandler::onChatMessage);

        // Anti-metagaming identity reveal ("my name is X" -> 50-block radius)
        NeoForge.EVENT_BUS.addListener(IdentityRevealHandler::onChatMessage);
        NeoForge.EVENT_BUS.addListener(IdentityRevealHandler::onPlayerLogin);

        // Anti-metagaming: suppress all player chat from the vanilla broadcast/log
        NeoForge.EVENT_BUS.addListener(ChatBroadcastSuppressor::onChatMessage);

        // Dodge i-frames
        NeoForge.EVENT_BUS.addListener(DodgeIFrameHandler::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.aotaddon.event.FemaleDecapitationHandler::onEyeHurt);

        // Combat tag
        NeoForge.EVENT_BUS.addListener(CombatTagHandler::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(CombatTagHandler::onServerTick);

        // Titan kill rewards (Honor/Medals-Banknotes/Rep display, popup text)
        NeoForge.EVENT_BUS.addListener(TitanKillRewardHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(TitanKillRewardHandler::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(CurrencySyncTicker::onServerTick);

        // Horse whistle bonding
        NeoForge.EVENT_BUS.addListener(HorseWhistleHandler::onEntityInteract);

        // Campfire regeneration
        NeoForge.EVENT_BUS.addListener(CampfireRegenHandler::onServerTick);

        // Shifter unlock potion handlers
        NeoForge.EVENT_BUS.addListener(ResilienceStaminaHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(ZeroHourExplosionHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(RaptorDashHandler::onServerTick);

        // Gas keybind
        modEventBus.addListener(GasCheckKeyHandler::registerKeyMapping);
        NeoForge.EVENT_BUS.addListener(GasCheckKeyHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(com.aotaddon.client.HookAttachLimitHandler::onClientTick);

        // Inventory tab bar — client only
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(new HelosHudRenderer());
            NeoForge.EVENT_BUS.register(new RewardPopupOverlay());
            NeoForge.EVENT_BUS.register(new CurrencyHudOverlay());
            NeoForge.EVENT_BUS.register(new PdSkullOverlay());
            NeoForge.EVENT_BUS.register(new CustomHeartOverlay());
            NeoForge.EVENT_BUS.register(new com.aotaddon.client.SkillCooldownOverlay());

            // Anti-metagaming: cancel nametag render for unrevealed players
            NeoForge.EVENT_BUS.addListener(IdentityNametagHandler::onRenderNameTag);

            ScreenTabHandler screenTabHandler = new ScreenTabHandler();
            NeoForge.EVENT_BUS.addListener(screenTabHandler::onScreenInit);
            NeoForge.EVENT_BUS.addListener(screenTabHandler::onScreenRender);

            new ChestTab().registerOnScreens();
            new XaerosMapTab().registerOnScreens();
            new FtbTeamsTab().registerOnScreens();
            new LegendarySurvivalTab().registerOnScreens();
            new SophisticatedBackpacksTab().registerOnScreens();

            // Placeholder tabs for not-yet-built features — visible now, inert on click.
            PlaceholderTab equipmentTab = new PlaceholderTab(
                    new ItemStack(Items.NETHERITE_CHESTPLATE), "Equipment (Coming Soon)");
            TabsMenu.addTabToScreen(equipmentTab, InventoryScreen.class, p -> 176, p -> 166, 50);

            PlaceholderTab teamsTab = new PlaceholderTab(
                    new ItemStack(Items.WHITE_BANNER), "Teams (Coming Soon)");
            TabsMenu.addTabToScreen(teamsTab, InventoryScreen.class, p -> 176, p -> 166, 60);
        }
        LOGGER.info("AoT Addon loaded.");
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID);

        registrar.playToServer(
                BastionTogglePayload.TYPE,
                BastionTogglePayload.STREAM_CODEC,
                BastionTogglePayload::handle
        );

        registrar.playToServer(
                ShiftlockTogglePayload.TYPE,
                ShiftlockTogglePayload.STREAM_CODEC,
                ShiftlockTogglePayload::handle
        );

        registrar.playToClient(
                ShiftlockStateSyncPayload.TYPE,
                ShiftlockStateSyncPayload.STREAM_CODEC,
                ShiftlockStateSyncPayload::handle
        );

        registrar.playToClient(
                HeadlessTitanSyncPayload.TYPE,
                HeadlessTitanSyncPayload.STREAM_CODEC,
                HeadlessTitanSyncPayload::handle
        );

        registrar.playToServer(
                DodgeStartPayload.TYPE,
                DodgeStartPayload.STREAM_CODEC,
                DodgeStartPayload::handle
        );

        registrar.playToServer(
                SkillEffectPayload.TYPE,
                SkillEffectPayload.STREAM_CODEC,
                SkillEffectPayload::handle
        );

        registrar.playToServer(
                TrailEffectPayload.TYPE,
                TrailEffectPayload.STREAM_CODEC,
                TrailEffectPayload::handle
        );

        registrar.playToServer(
                OdmGasXpPayload.TYPE,
                OdmGasXpPayload.STREAM_CODEC,
                OdmGasXpPayload::handle
        );

        registrar.playToServer(
                OpenGearPouchPayload.TYPE,
                OpenGearPouchPayload.STREAM_CODEC,
                OpenGearPouchPayload::handle
        );

        registrar.playToServer(
                HorseWhistlePayload.TYPE,
                HorseWhistlePayload.STREAM_CODEC,
                HorseWhistlePayload::handle
        );

        registrar.playToServer(
                ToggleConsentC2SPacket.TYPE,
                ToggleConsentC2SPacket.STREAM_CODEC,
                ToggleConsentC2SPacket::handle
        );

        registrar.playToServer(
                ToggleGrabModeC2SPacket.TYPE,
                ToggleGrabModeC2SPacket.STREAM_CODEC,
                ToggleGrabModeC2SPacket::handle
        );

        registrar.playToClient(
                RewardPopupPayload.TYPE,
                RewardPopupPayload.STREAM_CODEC,
                RewardPopupPayload::handle
        );

        registrar.playToClient(
                HonorSyncPayload.TYPE,
                HonorSyncPayload.STREAM_CODEC,
                HonorSyncPayload::handle
        );

        registrar.playToClient(
                CurrencySyncPayload.TYPE,
                CurrencySyncPayload.STREAM_CODEC,
                CurrencySyncPayload::handle
        );

        registrar.playToClient(
                RevealIdentityPayload.TYPE,
                RevealIdentityPayload.STREAM_CODEC,
                RevealIdentityPayload::handle
        );

        registrar.playToClient(
                IdentityFullSyncPayload.TYPE,
                IdentityFullSyncPayload.STREAM_CODEC,
                IdentityFullSyncPayload::handle
        );
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ODMDiagnosticKeybind.KEY_DIAGNOSE);
        event.register(ODMDiagnosticKeybind.KEY_BASTION);
        event.register(ODMDiagnosticKeybind.KEY_HORSE_WHISTLE);
        event.register(ODMDiagnosticKeybind.KEY_SHIFTLOCK);

        LOGGER.info("AoT Addon keybinds registered.");
    }
}