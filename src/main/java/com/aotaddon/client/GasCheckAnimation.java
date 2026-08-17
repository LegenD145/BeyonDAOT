package com.aotaddon.client;

import com.aotaddon.AotAddon;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Plays the local player's one-shot ODM gas-check gesture. */
public final class GasCheckAnimation {

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "odm_gas_check_layer");
    private static final ResourceLocation ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(AotAddon.MOD_ID, "odm_gas_check");

    private GasCheckAnimation() {}

    /**
     * Player Animator registration must be queued from NeoForge client setup;
     * registering it earlier can make the layer unavailable to the renderer.
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                LAYER_ID,
                1_000,
                player -> new PlayerAnimationController(
                        player,
                        (controller, state, animationSetter) -> PlayState.STOP
                )
        ));
    }

    public static void play(LocalPlayer player) {
        PlayerAnimationController controller = (PlayerAnimationController)
                PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
        if (controller != null) {
            controller.triggerAnimation(ANIMATION_ID);
        }
    }
}
