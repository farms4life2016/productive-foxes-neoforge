package io.github.farms4life2016.vixen_maid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BraixenModel extends EntityModel<Braixen> {
    private final ResourceLocation modelLocation;
    private BedrockGeoModel model;

    public BraixenModel(ResourceLocation modelLocation) {
        super(RenderType::entityCutout);
        this.modelLocation = modelLocation;
    }

    @Override
    public void setupAnim(Braixen entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        if (this.model == null) {
            this.model = BedrockGeoModel.load(this.modelLocation);
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        this.model.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
