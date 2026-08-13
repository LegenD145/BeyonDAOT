package com.aotaddon.client;
//ZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOS
import com.aotaddon.AotAddon;
import com.aotaddon.client.GearPouchClientSetup;
import com.aotaddon.network.BastionTogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AotAddon.MOD_ID, value = Dist.CLIENT)
public class ODMDashClientSetup {

    @SubscribeEvent
    public static void onClientTickEnd(ClientTickEvent.Post event) {
        ODMSkillHandler.tick();
        ODMWallClimbHandler.tick();
        tickBastion();
        tickDiagnostic();
        tickGearPouch();
        tickHorseWhistle();
        tickConsent();
        tickGrabMode();
        tickShiftlock();
        ShiftlockClientTick.tick();
    }

    // -------------------------------------------------------------------------
    // Horse whistle — press H to summon your bonded horse
    // -------------------------------------------------------------------------

    private static void tickHorseWhistle() {
        if (!ODMDiagnosticKeybind.KEY_HORSE_WHISTLE.consumeClick()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PacketDistributor.sendToServer(new com.aotaddon.network.HorseWhistlePayload());
    }

    // -------------------------------------------------------------------------
    // Gear Pouch — press G to open
    // -------------------------------------------------------------------------

    private static void tickGearPouch() {
        if (!GearPouchClientSetup.KEY_GEAR_POUCH.consumeClick()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        GearPouchClientSetup.onGearPouchKeyPressed();
    }

    // -------------------------------------------------------------------------
    // Bastion toggle — press B while in titan form
    // -------------------------------------------------------------------------

    private static void tickBastion() {
        if (!ODMDiagnosticKeybind.KEY_BASTION.consumeClick()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.sendSystemMessage(Component.literal("§e[Bastion] Sending toggle packet..."));
        PacketDistributor.sendToServer(new BastionTogglePayload());
    }

    // -------------------------------------------------------------------------
    // Shiftlock toggle — press C while in titan form
    // -------------------------------------------------------------------------

    private static void tickShiftlock() {
        if (!ODMDiagnosticKeybind.KEY_SHIFTLOCK.consumeClick()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PacketDistributor.sendToServer(new com.aotaddon.network.ShiftlockTogglePayload());
    }

    // -------------------------------------------------------------------------
    // Consent mode toggle
    // -------------------------------------------------------------------------

    private static void tickConsent() {
        if (!ConsentClientSetup.TOGGLE_CONSENT.consumeClick()) return;
        ConsentClientSetup.onConsentKeyPressed();
    }

    // -------------------------------------------------------------------------
    // Grab mode toggle
    // -------------------------------------------------------------------------

    private static void tickGrabMode() {
        if (!GrabModeClientSetup.TOGGLE_GRAB_MODE.consumeClick()) return;
        GrabModeClientSetup.onGrabModeKeyPressed();
    }

    // -------------------------------------------------------------------------
    // Diagnostic dump — press K while hooked to print ODM state to chat
    // -------------------------------------------------------------------------

    private static void tickDiagnostic() {
        if (!ODMDiagnosticKeybind.KEY_DIAGNOSE.consumeClick()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("§e=== ODM DIAGNOSTIC ===\n");

        // --- Gear check ---
        try {
            net.minecraft.world.item.ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
            sb.append("§fLegs item: §b").append(legs.isEmpty() ? "EMPTY" : legs.getItem().getClass().getName()).append("\n");

            if (!legs.isEmpty()) {
                Class<?> daot = Class.forName("daot.DannysAot");
                boolean isODM = (boolean) daot.getMethod("isODMGear", net.minecraft.world.item.Item.class)
                        .invoke(null, legs.getItem());
                sb.append("§fisODMGear: §").append(isODM ? "a" : "c").append(isODM).append("\n");

                boolean hasGas = (boolean) daot.getMethod("gearHasGas",
                                net.minecraft.world.item.ItemStack.class,
                                net.minecraft.world.entity.player.Player.class)
                        .invoke(null, legs, player);
                sb.append("§fhasGas: §").append(hasGas ? "a" : "c").append(hasGas).append("\n");
            }
        } catch (Exception e) {
            sb.append("§cGear check FAILED: ").append(e).append("\n");
        }

        // --- Hook state ---
        dumpHook(sb, true);
        dumpHook(sb, false);

        // --- Player state ---
        Vec3 vel = player.getDeltaMovement();
        double speed = vel.length();
        sb.append("§fSpeed: §b").append(String.format("%.4f", speed)).append("\n");
        sb.append("§fOnGround: §b").append(player.onGround()).append("\n");
        sb.append("§fVelocity: §b").append(String.format("%.3f, %.3f, %.3f", vel.x, vel.y, vel.z)).append("\n");

        // --- WallClimb internal state ---
        sb.append("§fwasPerching: §b").append(ODMWallClimbHandler.isCurrentlyPerching()).append("\n");

        // Print each line to chat
        for (String line : sb.toString().split("\n")) {
            player.sendSystemMessage(Component.literal(line));
        }
    }

    private static void dumpHook(StringBuilder sb, boolean left) {
        String side = left ? "LEFT" : "RIGHT";
        try {
            Class<?> tickHandler = Class.forName("daot.ODMTickHandler");
            Object hook = tickHandler.getMethod(left ? "getLeftHook" : "getRightHook").invoke(null);

            if (hook == null) {
                sb.append("§f").append(side).append(" hook: §cnull\n");
                return;
            }

            sb.append("§f").append(side).append(" hook class: §b").append(hook.getClass().getName()).append("\n");

            Class<?> hookClass = Class.forName("daot.HookPoint");
            boolean active     = hookClass.getField("active").getBoolean(hook);
            boolean extending  = hookClass.getField("isExtending").getBoolean(hook);
            boolean retracting = hookClass.getField("isRetracting").getBoolean(hook);
            Object pos         = hookClass.getField("position").get(hook);

            sb.append("§f").append(side).append(": active=§b").append(active)
                    .append("§f extending=§b").append(extending)
                    .append("§f retracting=§b").append(retracting).append("\n");

            if (pos == null) {
                sb.append("§f").append(side).append(" pos: §cnull\n");
            } else {
                sb.append("§f").append(side).append(" pos class: §b").append(pos.getClass().getName()).append("\n");
                if (pos instanceof Vec3 v) {
                    sb.append("§f").append(side).append(" pos: §b").append(String.format("%.2f, %.2f, %.2f", v.x, v.y, v.z)).append("\n");
                } else {
                    // Try to read x/y/z by field name in case remapping didn't happen
                    try {
                        double x = pos.getClass().getField("x").getDouble(pos);
                        double y = pos.getClass().getField("y").getDouble(pos);
                        double z = pos.getClass().getField("z").getDouble(pos);
                        sb.append("§f").append(side).append(" pos (x/y/z fields): §b")
                                .append(String.format("%.2f, %.2f, %.2f", x, y, z)).append("\n");
                    } catch (Exception e2) {
                        sb.append("§c").append(side).append(" pos NOT Vec3, class=")
                                .append(pos.getClass().getName()).append(", x/y/z fields failed: ").append(e2.getMessage()).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            sb.append("§c").append(side).append(" hook FAILED: ").append(e).append("\n");
        }
    }
}