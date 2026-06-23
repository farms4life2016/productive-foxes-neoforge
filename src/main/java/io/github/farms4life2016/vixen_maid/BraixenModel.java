/*
 * animation logic adapted from these files: 
 * - https://gitlab.com/cable-mc/cobblemon/-/tree/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/pokemon/gen6/BraixenModel.kt
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class BraixenModel extends EntityModel<Braixen> {
    private static final String IDLE_ANIMATION = "animation.braixen.ground_idle";
    private static final String WALK_ANIMATION = "animation.braixen.ground_walk";
    private static final String BLINK_ANIMATION = "animation.braixen.blink";
    private static final String MUNCH_ANIMATION = "animation.braixen.munch";
    private static final boolean FORCE_IDLE_DURING_MUNCH = false;
    private static final float MAX_EATING_HEAD_YAW = 10.0F;
    private static final double MOVING_SPEED_THRESHOLD = 1.0E-5D;
    private static final float MIN_SECONDS_BETWEEN_BLINKS = 8.0F;
    private static final float MAX_SECONDS_BETWEEN_BLINKS = 30.0F;
    private static final float MODEL_Y_OFFSET = 1.5F;
    private static final String LEASH_BONE = "neck";
    private static final Vec3 LEASH_BONE_OFFSET_PIXELS = new Vec3(0.0D, -2.0D, 0.0D);
    private static final String MOUTH_BONE = "muzzle";
    private static final Vec3 MOUTH_BONE_OFFSET_PIXELS = new Vec3(0.0D, 0.0D, 0.25D);
    public static final String HELD_ITEM_BONE = "hand_left";
    private static final Vec3 HELD_ITEM_OFFSET_PIXELS = new Vec3(1.5D, 0.5D, 0.0D);

    private final ResourceLocation modelLocation;
    private final ResourceLocation[] animationLocations;
    private final Map<Integer, BlinkState> blinkStates = new HashMap<>();
    private BedrockGeoModel model;
    private Braixen currentEntity;
    private String currentAnimation = IDLE_ANIMATION;
    private float currentAnimationSeconds;
    private float blinkAnimationSeconds = -1.0F;
    private float munchAnimationSeconds = -1.0F;
    private float netHeadYaw;
    private float headPitch;

    public BraixenModel(ResourceLocation modelLocation, ResourceLocation... animationLocations) {
        super(RenderType::entityCutout);
        this.modelLocation = modelLocation;
        this.animationLocations = animationLocations;
    }

    @Override
    public void setupAnim(Braixen entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.currentEntity = entity;
        boolean moving = limbSwingAmount > 0.01F || entity.getDeltaMovement().horizontalDistanceSqr() > MOVING_SPEED_THRESHOLD;
        this.currentAnimationSeconds = ageInTicks / 20.0F;
        this.munchAnimationSeconds = entity.getEatingAnimationSeconds(ageInTicks);
        this.currentAnimation = this.munchAnimationSeconds >= 0.0F && FORCE_IDLE_DURING_MUNCH ? IDLE_ANIMATION : moving ? WALK_ANIMATION : IDLE_ANIMATION;
        this.blinkAnimationSeconds = this.updateBlinkState(entity, this.currentAnimationSeconds);
        this.netHeadYaw = this.munchAnimationSeconds >= 0.0F ? Mth.clamp(netHeadYaw, -MAX_EATING_HEAD_YAW, MAX_EATING_HEAD_YAW) : netHeadYaw;
        this.headPitch = headPitch;

        if (this.model != null) {
            this.applyCurrentPose();
            this.updateEntityAnchors(entity);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        if (this.model == null) {
            this.model = BedrockGeoModel.load(this.modelLocation, this.animationLocations);
        }
        this.applyCurrentPose();
        if (this.currentEntity != null) {
            this.updateEntityAnchors(this.currentEntity);
            this.currentEntity.spawnPendingClientEatingParticles();
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);
        this.model.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    public boolean translateToHeldItem(PoseStack poseStack) {
        return this.translateToBone(poseStack, HELD_ITEM_BONE, HELD_ITEM_OFFSET_PIXELS);
    }

    public boolean translateToBone(PoseStack poseStack, String boneName, Vec3 offsetPixels) {
        if (this.model == null) {
            this.model = BedrockGeoModel.load(this.modelLocation, this.animationLocations);
        }

        this.applyCurrentPose();
        poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);
        return this.model.transformToPart(poseStack, boneName, offsetPixels);
    }

    private void applyCurrentPose() {
        this.model.applyAnimation(this.currentAnimation, this.currentAnimationSeconds);
        if (this.blinkAnimationSeconds >= 0.0F) {
            this.model.applyAnimationLayer(BLINK_ANIMATION, this.blinkAnimationSeconds);
        }
        if (this.munchAnimationSeconds >= 0.0F) {
            this.model.applyAnimationLayer(MUNCH_ANIMATION, this.munchAnimationSeconds);
        }
        this.model.lookAt("head", this.netHeadYaw, this.headPitch);
        this.model.setVisible("hand_stick", false);
        this.model.setVisible("stick_tail", true);
    }

    private void updateEntityAnchors(Braixen entity) {
        Vec3 leashOffset = this.entityLocalBoneOrigin(LEASH_BONE, LEASH_BONE_OFFSET_PIXELS);
        Vec3 mouthOffset = this.entityLocalBoneOrigin(MOUTH_BONE, MOUTH_BONE_OFFSET_PIXELS);
        if (leashOffset != null || mouthOffset != null) {
            entity.setClientAnchorOffsets(leashOffset, mouthOffset);
        }
    }

    private Vec3 entityLocalBoneOrigin(String boneName, Vec3 offsetPixels) {
        Vector4f origin = this.model.partOrigin(boneName, offsetPixels);
        if (origin == null) {
            return null;
        }

        // ModelPart space is mirrored from entity offset space: +Y is down and -Z is forward.
        return new Vec3(-origin.x(), -origin.y(), -origin.z());
    }

    private float updateBlinkState(Braixen entity, float animationSeconds) {
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

    private float nextBlinkDelay(Braixen entity) {
        return MIN_SECONDS_BETWEEN_BLINKS
                + entity.getRandom().nextFloat() * (MAX_SECONDS_BETWEEN_BLINKS - MIN_SECONDS_BETWEEN_BLINKS);
    }

    private static final class BlinkState {
        private float nextBlinkSeconds = -1.0F;
        private float blinkStartSeconds = -1.0F;
    }
}
