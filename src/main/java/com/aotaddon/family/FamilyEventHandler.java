package com.aotaddon.family;

import com.aotaddon.AotAddon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import com.aotaddon.currency.MedalData;
import com.aotaddon.currency.BanknoteData;
import com.aotaddon.currency.CurrencyFaction;
import java.util.List;
import java.util.Set;

/**
 * Handles all server-side family ability logic.
 *
 * NOT annotated with @EventBusSubscriber — registered manually via
 * NeoForge.EVENT_BUS.addListener() in AotAddon constructor AFTER the
 * mixin preparation phase, so Entity is never forced to load too early.
 *
 * All entity/event class references are kept inside method bodies only.
 */
public class FamilyEventHandler {

    private static final Set<String> PURE_TITAN_IDS = Set.of(
            "dannys-aot:titan", "dannys-aot:small_titan", "dannys-aot:small_titan_2",
            "dannys-aot:sad_titan", "dannys-aot:titan_tropical", "dannys-aot:yellow_titan",
            "dannys-aot:connie_father", "dannys-aot:abnormal_titan", "dannys-aot:crawler_titan",
            "dannys-aot:crawling_abnormal_titan", "dannys-aot:fritz_titan", "dannys-aot:titan_beard"
    );

    private static final Set<String> NAPE_IDS = Set.of(
            "dannys-aot:titan_nape", "dannys-aot:small_titan_nape", "dannys-aot:small_titan_2_nape",
            "dannys-aot:sad_titan_nape", "dannys-aot:titan_tropical_nape", "dannys-aot:yellow_titan_nape",
            "dannys-aot:connie_father_nape", "dannys-aot:abnormal_titan_nape", "dannys-aot:crawler_titan_nape",
            "dannys-aot:crawling_abnormal_titan_nape", "dannys-aot:fritz_titan_nape", "dannys-aot:titan_beard_nape"
    );

    private static final Set<String> EYE_IDS = Set.of(
            "dannys-aot:titan_eye", "dannys-aot:small_titan_eye", "dannys-aot:small_titan_2_eye",
            "dannys-aot:sad_titan_eye", "dannys-aot:titan_tropical_eye", "dannys-aot:yellow_titan_eye",
            "dannys-aot:connie_father_eye", "dannys-aot:crawler_titan_eye",
            "dannys-aot:fritz_titan_eye", "dannys-aot:titan_beard_eye"
    );

    private static final int STUN_TICKS = 60;
    public static final double HELOS_WIPE_RADIUS = 20.0;
    public static final double ROYAL_STUN_RADIUS = 50.0;

    // =========================================================================
    // EVENT HANDLERS — registered via addListener() in AotAddon, not @SubscribeEvent
    // =========================================================================

    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String family = FamilyData.getFamily(player);
        if (family != null && !family.isEmpty()) {
            player.getServer().execute(() -> sendFamilyMessage(player, family));
        }
    }
    // Tracks active welcome windows — player name → expiry time (ms)
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> WELCOME_WINDOWS =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void onPlayerFirstJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Already rewarded - nothing more to do regardless of bloodline.
        if (player.getPersistentData().getBoolean("startingBonusGranted")) return;

        // Bloodline is only ever set via /setbloodline while a player is
        // online, so it's never tagged yet at a brand new player's literal
        // first join. Instead of gating on "first join ever", we check
        // EVERY login and grant once we see a tagged bloodline that hasn't
        // been rewarded yet - covers "tagged before first join" (impossible
        // today, but harmless if that ever changes) and the realistic case
        // of "tagged sometime after joining, reward lands on their next login".
        CurrencyFaction.Faction faction = CurrencyFaction.get(player);
        if (faction == CurrencyFaction.Faction.NONE) return;

        player.getPersistentData().putBoolean("startingBonusGranted", true);

        String currencyName;
        if (faction == CurrencyFaction.Faction.MARLEY) {
            BanknoteData.addBalance(player, 2500);
            currencyName = "banknotes";
        } else {
            MedalData.addBalance(player, 2500);
            currencyName = "medals";
        }

        player.sendSystemMessage(Component.literal(
                        "§6[Welcome] §fYou have been granted §e2500 " + currencyName + "§f as a starting bonus!")
                .withStyle(ChatFormatting.GOLD));

        // Broadcast global welcome message
        String playerName = player.getName().getString();
        WELCOME_WINDOWS.put(playerName, System.currentTimeMillis() + (2 * 60 * 1000L));

        player.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§e[Server] §f" + playerName +
                        " has joined the server, say §awelcome " + playerName +
                        "§f to them to gain a reward!"), false);
    }

    public static void onChatMessage(net.neoforged.neoforge.event.ServerChatEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sender)) return;

        String message = event.getMessage().getString().trim();
        long now = System.currentTimeMillis();

        // Clean expired windows first
        WELCOME_WINDOWS.entrySet().removeIf(e -> now > e.getValue());

        for (String newPlayerName : WELCOME_WINDOWS.keySet()) {
            // Don't let the new player welcome themselves
            if (sender.getName().getString().equalsIgnoreCase(newPlayerName)) continue;

            if (message.equalsIgnoreCase("welcome " + newPlayerName)) {
                Long expiry = WELCOME_WINDOWS.get(newPlayerName);
                if (expiry != null && now <= expiry) {
                    CurrencyFaction.Faction senderFaction = CurrencyFaction.get(sender);
                    if (senderFaction == CurrencyFaction.Faction.MARLEY) {
                        BanknoteData.addBalance(sender, 1);
                        sender.sendSystemMessage(Component.literal(
                                        "§a[Welcome] §fYou earned §e1 banknote§f for welcoming " + newPlayerName + "!")
                                .withStyle(ChatFormatting.GREEN));
                    } else if (senderFaction == CurrencyFaction.Faction.ELDIAN) {
                        MedalData.addBalance(sender, 1);
                        sender.sendSystemMessage(Component.literal(
                                        "§a[Welcome] §fYou earned §e1 medal§f for welcoming " + newPlayerName + "!")
                                .withStyle(ChatFormatting.GREEN));
                    }
                    // NONE faction: no reward, since we don't know which currency to grant
                }
                break;
            }
        }
    }

    public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        net.minecraft.world.entity.LivingEntity dying = event.getEntity();
        net.minecraft.world.damagesource.DamageSource source = event.getSource();

        // Helos kill counter
        if (isPureTitan(dying) && source.getEntity() instanceof ServerPlayer attacker) {
            if (FamilyData.isHelos(attacker)) {
                int newKills = FamilyData.incrementHelosKills(attacker);
                AotAddon.LOGGER.debug("[TitanRequiem] Helos kills: {}/{} for {}",
                        newKills, FamilyData.HELOS_MAX_KILLS, attacker.getName().getString());
            }
        }

        // Fritz/Yeager revive
        if (dying instanceof ServerPlayer player && FamilyData.hasRevive(player)) {
            if (!FamilyData.isReviveOnCooldown(player)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth() / 2f);
                FamilyData.setReviveTimestamp(player, System.currentTimeMillis());

                String family = FamilyData.getFamily(player);
                String msg = "fritz".equals(family)
                        ? "§6[Fritz] §rYour royal blood refuses to fade."
                        : "§6[Yeager] §rYou keep moving forward.";
                player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    // =========================================================================
    // TITAN STUN
    // =========================================================================

    public static void stunTitansNearby(ServerPlayer player) {
        if (!FamilyData.hasRoyalBlood(player)) return;
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(ROYAL_STUN_RADIUS);
        List<net.minecraft.world.entity.Mob> nearby = level.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, box,
                e -> isPureTitan(e) && !e.getUUID().equals(player.getUUID())
        );
        for (net.minecraft.world.entity.Mob titan : nearby) {
            try { titan.setNoAi(true); scheduleAiRestore(player, titan, STUN_TICKS); }
            catch (Exception e) { AotAddon.LOGGER.error("[TitanRequiem] stunTitansNearby failed: {}", e.getMessage()); }
        }
    }

    private static void scheduleAiRestore(ServerPlayer player, net.minecraft.world.entity.Mob titan, int ticks) {
        if (ticks <= 0) { player.getServer().execute(() -> titan.setNoAi(false)); return; }
        player.getServer().execute(() -> scheduleAiRestore(player, titan, ticks - 1));
    }

    // =========================================================================
    // HELOS WIPE
    // =========================================================================

    public static void executeHelösWipe(ServerPlayer player, double x, double y, double z) {
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        AABB box = new AABB(x, y, z, x, y, z).inflate(HELOS_WIPE_RADIUS);
        List<net.minecraft.world.entity.LivingEntity> titans = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, box,
                e -> isPureTitan(e) && !e.getUUID().equals(player.getUUID())
        );
        for (net.minecraft.world.entity.LivingEntity titan : titans) {
            try { titan.kill(); }
            catch (Exception e) { AotAddon.LOGGER.error("[TitanRequiem] Wipe kill failed: {}", e.getMessage()); }
        }
        FamilyData.resetHelosKills(player);
        player.displayClientMessage(
                Component.literal("§c[Helos] §rThe drums of liberation have silenced them.").withStyle(ChatFormatting.DARK_RED), true);
    }

    // =========================================================================
    // FAMILY MESSAGE
    // =========================================================================

    public static void sendFamilyMessage(ServerPlayer player, String family) {
        String msg = switch (family.toLowerCase()) {
            case "helos"  -> "The drums of liberation beat through your heart. You no longer fear the Devils.";
            case "fritz"  -> "一度奴隷にされたら、永遠に奴隷のままだ。";
            case "reiss"  -> "To the lands of freedom you're a ruler, to the sea you're the Devil.";
            case "yeager" -> "私は前進し続けなければならない";
            default -> null;
        };
        if (msg == null) return;
        player.displayClientMessage(Component.literal("§8[Family] §r" + msg).withStyle(ChatFormatting.WHITE), true);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    public static boolean isPureTitan(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        try { return PURE_TITAN_IDS.contains(entity.getType().builtInRegistryHolder().key().location().toString()); }
        catch (Exception e) { return false; }
    }

    public static boolean isNapeOrEye(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        try {
            String id = entity.getType().builtInRegistryHolder().key().location().toString();
            return NAPE_IDS.contains(id) || EYE_IDS.contains(id);
        } catch (Exception e) { return false; }
    }
}