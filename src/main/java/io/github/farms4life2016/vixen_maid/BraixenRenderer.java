package io.github.farms4life2016.vixen_maid;

import io.github.farms4life2016.ProductiveFoxes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BraixenRenderer extends MobRenderer<Braixen, BraixenModel> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "bedrock/vixen_maid/model/braixen.geo.json"
    );
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "bedrock/vixen_maid/anim/braixen.animation.json"
    );
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "textures/entity/vixen_maid/braixen.png"
    );

    public BraixenRenderer(EntityRendererProvider.Context context) {
        super(context, new BraixenModel(MODEL, ANIMATION), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Braixen entity) {
        return TEXTURE;
    }
}
