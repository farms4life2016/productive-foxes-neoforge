package io.github.farms4life2016.vixen_maid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BraixenModel extends EntityModel<Braixen> {
    private static final String IDLE_ANIMATION = "animation.braixen.ground_idle";
    private static final String WALK_ANIMATION = "animation.braixen.ground_walk";
    private static final double MOVING_SPEED_THRESHOLD = 1.0E-5D;

    private final ResourceLocation modelLocation;
    private final ResourceLocation animationLocation;
    private BedrockGeoModel model;
    private String currentAnimation = IDLE_ANIMATION;
    private float currentAnimationSeconds;
    private float netHeadYaw;
    private float headPitch;

    public BraixenModel(ResourceLocation modelLocation, ResourceLocation animationLocation) {
        super(RenderType::entityCutout);
        this.modelLocation = modelLocation;
        this.animationLocation = animationLocation;
    }

    @Override
    public void setupAnim(Braixen entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean moving = limbSwingAmount > 0.01F || entity.getDeltaMovement().horizontalDistanceSqr() > MOVING_SPEED_THRESHOLD;
        this.currentAnimation = moving ? WALK_ANIMATION : IDLE_ANIMATION;
        this.currentAnimationSeconds = ageInTicks / 20.0F;
        this.netHeadYaw = netHeadYaw;
        this.headPitch = headPitch;

        if (this.model != null) {
            this.applyCurrentPose();
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        if (this.model == null) {
            this.model = BedrockGeoModel.load(this.modelLocation, this.animationLocation);
        }
        this.applyCurrentPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        this.model.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    private void applyCurrentPose() {
        this.model.applyAnimation(this.currentAnimation, this.currentAnimationSeconds);
        this.model.lookAt("head", this.netHeadYaw, this.headPitch);
        this.model.setVisible("hand_stick", false);
        this.model.setVisible("stick_tail", true);
    }
}
