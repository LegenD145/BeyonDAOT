package com.aotaddon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.aotaddon.combat.SeveredPartEntity;
import com.aotaddon.combat.SeveredPartGeoModel;
import com.aotaddon.combat.ShifterTitanHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders only the detached head bone from the parent shifter's DAOT geo file.
 */
public class SeveredPartRenderer extends GeoEntityRenderer<SeveredPartEntity> {

    public SeveredPartRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        super(context, new SeveredPartGeoModel());
        this.shadowRadius = 1.5f;
    }

    @Override
    public void render(SeveredPartEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        try {
            float spin = entity.getSpinDegrees(partialTick);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin * 0.55f));

            double anchor = ShifterTitanHelper.severedHeadRenderAnchor(entity.getTitanClassName());
            poseStack.translate(0.0, -anchor, 0.0);

            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SeveredPartEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (!isHeadTreeBone(bone, animatable.getBoneName())) {
            return;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    private static boolean isHeadTreeBone(GeoBone bone, String rootBoneName) {
        GeoBone current = bone;
        while (current != null) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
