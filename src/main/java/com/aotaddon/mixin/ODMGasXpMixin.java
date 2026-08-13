package com.aotaddon.mixin;

import com.aotaddon.AotAddon;
import com.aotaddon.network.OdmGasXpPayload;
import com.aotaddon.util.OdmXpHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grants ODM skill-tree XP after DAOT successfully consumes gas.
 *
 * Normal DAOT ODM movement spends gas on the client, then syncs gas to the
 * server. For that path, this mixin sends a small addon packet to the server.
 *
 * If another path ever spends gas on the server, this grants XP directly.
 */
@Mixin(targets = "daot.DannysAot", remap = false)
public class ODMGasXpMixin {

    @Inject(
            method = "consumeGasFromGear(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/player/Player;)Z",
            at = @At("RETURN"),
            remap = false
    )
    private static void onConsumeGasFromGear(ItemStack stack, int amount, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (amount <= 0) return;
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;

        if (player.level().isClientSide()) {
            AotAddon.LOGGER.debug("[ODMGasXpMixin] Sending ODM XP packet for {} gas", amount);
            PacketDistributor.sendToServer(new OdmGasXpPayload(amount));
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            OdmXpHandler.grantGasXp(serverPlayer, amount);
        }
    }
}
