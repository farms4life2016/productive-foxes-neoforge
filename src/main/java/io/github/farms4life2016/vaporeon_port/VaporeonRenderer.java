package io.github.farms4life2016.vaporeon_port;

import io.github.farms4life2016.ProductiveFoxes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Productive Foxes code. Renders through VaporeonModel, which uses the MPL-2.0 BedrockGeoModel adapter.
public class VaporeonRenderer extends MobRenderer<VaporeonEntity, VaporeonModel> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "bedrock/vaporeon/vaporeon.geo.json"
    );
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ProductiveFoxes.MODID,
            "textures/entity/vaporeon/vaporeon.png"
    );

    public VaporeonRenderer(EntityRendererProvider.Context context) {
        super(context, new VaporeonModel(MODEL), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(VaporeonEntity entity) {
        return TEXTURE;
    }
}
