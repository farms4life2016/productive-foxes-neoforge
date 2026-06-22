package io.github.farms4life2016.vixen_maid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BraixenHeldItemLayer extends RenderLayer<Braixen, BraixenModel> {
    private final ItemInHandRenderer itemInHandRenderer;

    public BraixenHeldItemLayer(RenderLayerParent<Braixen, BraixenModel> renderer, ItemInHandRenderer itemInHandRenderer) {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Braixen livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        ItemStack itemStack = livingEntity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (itemStack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        if (this.getParentModel().translateToHeldItem(poseStack)) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.itemInHandRenderer.renderItem(livingEntity, itemStack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }
}
