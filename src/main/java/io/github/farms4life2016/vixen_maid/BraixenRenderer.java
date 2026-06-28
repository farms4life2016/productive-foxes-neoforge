package io.github.farms4life2016.vixen_maid;

import io.github.farms4life2016.ProductiveFoxes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BraixenRenderer extends MobRenderer<Braixen, BraixenModel> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "vixen_maid/ccpl/braixen.geo.json"
    );
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "vixen_maid/ccpl/braixen.animation.json"
    );
    private static final ResourceLocation MUNCH_ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "vixen_maid/custom/braixen_munch.animation.json"
    );
    private static final ResourceLocation YELLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "vixen_maid/ccpl/braixen_yellow.png"
    );
    private static final ResourceLocation LAVENDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "vixen_maid/ccpl/braixen_lavender.png"
    );

    public BraixenRenderer(EntityRendererProvider.Context context) {
        super(context, new BraixenModel(MODEL, ANIMATION, MUNCH_ANIMATION), 0.5F);
        this.addLayer(new BraixenHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Braixen entity) {
        return entity.getVariant() == Braixen.Type.LAVENDER ? LAVENDER_TEXTURE : YELLOW_TEXTURE;
    }
}
