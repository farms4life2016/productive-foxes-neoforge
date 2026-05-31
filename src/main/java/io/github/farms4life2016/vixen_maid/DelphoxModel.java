/*
 * animation logic adapted from these files:
 * - https://gitlab.com/cable-mc/cobblemon/-/tree/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/pokemon/gen6/DelphoxModel.kt
 * - https://gitlab.com/cable-mc/cobblemon/-/blob/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/quirk/SimpleQuirk.kt
 * - https://gitlab.com/cable-mc/cobblemon/-/blob/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/quirk/QuirkAnimation.kt
 *
 * idk maybe this file should be MPL 2.0 too...
 */

package io.github.farms4life2016.vixen_maid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class DelphoxModel extends EntityModel<Delphox> {
    private static final String IDLE_ANIMATION = "animation.delphox.ground_idle";
    private static final String WALK_ANIMATION = "animation.delphox.ground_walk";
    private static final String BLINK_ANIMATION = "animation.delphox.blink";
    private static final double MOVING_SPEED_THRESHOLD = 1.0E-5D;
    private static final float MIN_SECONDS_BETWEEN_BLINKS = 8.0F;
    private static final float MAX_SECONDS_BETWEEN_BLINKS = 30.0F;

    private final ResourceLocation modelLocation;
    private final ResourceLocation animationLocation;
    private final Map<Integer, BlinkState> blinkStates = new HashMap<>();
    private BedrockGeoModel model;
    private String currentAnimation = IDLE_ANIMATION;
    private float currentAnimationSeconds;
    private float blinkAnimationSeconds = -1.0F;
    private float netHeadYaw;
    private float headPitch;

    public DelphoxModel(ResourceLocation modelLocation, ResourceLocation animationLocation) {
        super(RenderType::entityCutout);
        this.modelLocation = modelLocation;
        this.animationLocation = animationLocation;
    }

    @Override
    public void setupAnim(Delphox entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean moving = limbSwingAmount > 0.01F || entity.getDeltaMovement().horizontalDistanceSqr() > MOVING_SPEED_THRESHOLD;
        this.currentAnimation = moving ? WALK_ANIMATION : IDLE_ANIMATION;
        this.currentAnimationSeconds = ageInTicks / 20.0F;
        this.blinkAnimationSeconds = this.updateBlinkState(entity, this.currentAnimationSeconds);
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
        if (this.blinkAnimationSeconds >= 0.0F) {
            this.model.applyAnimationLayer(BLINK_ANIMATION, this.blinkAnimationSeconds);
        }
        this.model.lookAt("head", this.netHeadYaw, this.headPitch);
        this.model.setVisible("hand_stick", false);
    }

    private float updateBlinkState(Delphox entity, float animationSeconds) {
        BlinkState state = this.blinkStates.computeIfAbsent(entity.getId(), unused -> new BlinkState());
        if (state.nextBlinkSeconds < 0.0F) {
            state.nextBlinkSeconds = animationSeconds + this.nextBlinkDelay(entity);
        }

        float blinkLength = this.model != null ? this.model.getAnimationLength(BLINK_ANIMATION) : 0.125F;
        if (state.blinkStartSeconds >= 0.0F) {
            float blinkSeconds = animationSeconds - state.blinkStartSeconds;
            if (blinkSeconds < blinkLength) {
                return blinkSeconds;
            }

            state.blinkStartSeconds = -1.0F;
            state.nextBlinkSeconds = animationSeconds + this.nextBlinkDelay(entity);
            return -1.0F;
        }

        if (animationSeconds >= state.nextBlinkSeconds) {
            state.blinkStartSeconds = animationSeconds;
            return 0.0F;
        }
        return -1.0F;
    }

    private float nextBlinkDelay(Delphox entity) {
        return MIN_SECONDS_BETWEEN_BLINKS
                + entity.getRandom().nextFloat() * (MAX_SECONDS_BETWEEN_BLINKS - MIN_SECONDS_BETWEEN_BLINKS);
    }

    private static final class BlinkState {
        private float nextBlinkSeconds = -1.0F;
        private float blinkStartSeconds = -1.0F;
    }
}
