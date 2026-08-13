package com.aotaddon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.aotaddon.combat.SeveredPartEntity;
import com.aotaddon.combat.SeveredPartGeoModel;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Optional;

/**
 * Renders a SeveredPartEntity by hiding every bone in Female Titan's model
 * except the one named on the entity (and that bone's children), each
 * frame, then letting GeoEntityRenderer's normal render pass draw whatever
 * is left visible. Since SeveredPartEntity registers no animation
 * controllers, every visible bone stays in its geo-file bind pose.
 *
 * GeckoLib 4.8's setHidden(true) also hides children, so hidden parent
 * bones are forced to keep child traversal enabled while this renderer is
 * drawing a detached bone.
 */
public class SeveredPartRenderer extends GeoEntityRenderer<SeveredPartEntity> {

    private static final double FEMALE_HEAD_BONE_ANCHOR_Y = 11.0;

    public SeveredPartRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        super(context, new SeveredPartGeoModel());
        this.shadowRadius = 2.0f;
    }

    @Override
    public void render(SeveredPartEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight) {
        BakedGeoModel bakedModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(entity));
        applyBoneVisibility(bakedModel, entity.getBoneName());

        poseStack.pushPose();
        try {
            float spin = entity.getSpinDegrees(partialTick);
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin * 0.55f));
            poseStack.translate(0.0, -FEMALE_HEAD_BONE_ANCHOR_Y, 0.0);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            poseStack.popPose();
            restoreBoneVisibility(bakedModel);
        }
    }

    @Override
    protected void applyRotations(SeveredPartEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        super.applyRotations(animatable, poseStack, ageInTicks, yaw, partialTick, nativeScale);
    }

    private static void applyBoneVisibility(BakedGeoModel model, String targetBoneName) {
        for (GeoBone root : model.topLevelBones()) {
            hideSelfOnlyRecursively(root);
        }

        Optional<GeoBone> target = model.getBone(targetBoneName);
        target.ifPresent(SeveredPartRenderer::showRecursively);
    }

    private static void hideSelfOnlyRecursively(GeoBone bone) {
        bone.setHidden(true);
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            hideSelfOnlyRecursively(child);
        }
    }

    private static void showRecursively(GeoBone bone) {
        bone.setHidden(Boolean.TRUE.equals(bone.shouldNeverRender()));
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            showRecursively(child);
        }
    }

    private static void restoreBoneVisibility(BakedGeoModel model) {
        for (GeoBone root : model.topLevelBones()) {
            restoreBoneVisibility(root);
        }
    }

    private static void restoreBoneVisibility(GeoBone bone) {
        bone.setHidden(Boolean.TRUE.equals(bone.shouldNeverRender()));
        bone.setChildrenHidden(false);
        for (GeoBone child : bone.getChildBones()) {
            restoreBoneVisibility(child);
        }
    }
}
