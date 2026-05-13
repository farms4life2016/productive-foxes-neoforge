package io.github.farms4life2016.shoulder_mount;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.FoxModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;

public class FoxShoulderLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
    private static final ResourceLocation RED_FOX_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fox/fox.png");
    private static final ResourceLocation SNOW_FOX_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fox/snow_fox.png");

    private final FoxModel<Fox> babyFoxModel;

    public FoxShoulderLayer(RenderLayerParent<T, PlayerModel<T>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.babyFoxModel = new FoxModel<>(modelSet.bakeLayer(ModelLayers.FOX));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        renderShoulderFox(poseStack, bufferSource, packedLight, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, true);
        renderShoulderFox(poseStack, bufferSource, packedLight, player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, false);
    }

    private void renderShoulderFox(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T player,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            boolean left
    ) {
        CompoundTag foxData = left ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();

        if (foxData.isEmpty() || !BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.FOX).toString().equals(foxData.getString("id"))) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(left ? 0.4F : -0.4F, player.isCrouching() ? -1.3F : -1.5F, -0.1F);

        Fox shoulderFox = EntityType.FOX.create(player.level());
        if (shoulderFox != null) {
            shoulderFox.load(foxData);

            ResourceLocation texture = shoulderFox.getVariant() == Fox.Type.RED ? RED_FOX_TEXTURE : SNOW_FOX_TEXTURE;
            var consumer = bufferSource.getBuffer(this.babyFoxModel.renderType(texture));

            this.babyFoxModel.prepareMobModel(shoulderFox, limbSwing, limbSwingAmount, 0.0F);
            this.babyFoxModel.setupAnim(shoulderFox, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            this.babyFoxModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }
}
