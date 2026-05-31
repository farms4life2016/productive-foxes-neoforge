package io.github.farms4life2016.vixen_maid;

import io.github.farms4life2016.ProductiveFoxes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DelphoxRenderer extends MobRenderer<Delphox, DelphoxModel> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "bedrock/vixen_maid/model/delphox.geo.json"
    );
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "bedrock/vixen_maid/anim/delphox.animation.json"
    );
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "textures/entity/vixen_maid/delphox.png"
    );

    public DelphoxRenderer(EntityRendererProvider.Context context) {
        super(context, new DelphoxModel(MODEL, ANIMATION), 0.5F); // verify with cobblemon that shadow radius is correct
    }

    @Override
    public ResourceLocation getTextureLocation(Delphox entity) {
        return TEXTURE;
    }
}
