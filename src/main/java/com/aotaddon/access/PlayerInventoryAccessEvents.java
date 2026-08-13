package com.aotaddon.access;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "titanreqiuem")
public class PlayerInventoryAccessEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer initiator)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (initiator.getUUID().equals(target.getUUID())) {
            return;
        }
        if (!initiator.getMainHandItem().isEmpty() || !initiator.getOffhandItem().isEmpty()) {
            return;
        }

        boolean cuffed = DaotBridge.isCuffed(target);
        boolean consented = ConsentManager.get(initiator.getServer()).isOpen(target);

        if (!cuffed && !consented) {
            initiator.displayClientMessage(
                    Component.literal("Player hasn't consented").withStyle(ChatFormatting.RED),
                    true
            );
            event.setCanceled(true);
            return;
        }

        initiator.openMenu(
                new SimpleMenuProvider(
                        (windowId, viewerInv, viewer) -> new PlayerInventoryAccessMenu(windowId, viewerInv, target.getId()),
                        Component.literal(target.getName().getString() + "'s Inventory")
                ),
                buf -> buf.writeVarInt(target.getId())
        );
        event.setCanceled(true);
    }
}