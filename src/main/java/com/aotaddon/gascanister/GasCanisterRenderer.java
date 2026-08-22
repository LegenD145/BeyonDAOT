package com.aotaddon.gascanister;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib block renderer for the large gas canister. Model is Blockbench pixels —
 * center on the block and scale 1/16 to match Minecraft block units.
 */
public class GasCanisterRenderer extends GeoBlockRenderer<GasCanisterBlockEntity> {

    /** Model body center in Blockbench pixels (see gas_block.geo.json). */
    private static final float CENTER_X = 5.875f;
    private static final float CENTER_Z = 4.125f;

    public GasCanisterRenderer() {
        super(new GasCanisterGeoModel());
        withScale(1f / 16f);
    }

    @Override
    public void preRender(PoseStack poseStack, GasCanisterBlockEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.translate(-CENTER_X / 16f, 0.0, -CENTER_Z / 16f);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }
}
