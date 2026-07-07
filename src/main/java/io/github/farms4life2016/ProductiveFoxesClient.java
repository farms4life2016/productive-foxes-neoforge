package io.github.farms4life2016;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.farms4life2016.client.ThreeToneFluidRenderer;
import io.github.farms4life2016.vixen_maid.BraixenRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.joml.Vector3f;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ProductiveFoxes.MODID, dist = Dist.CLIENT)
public class ProductiveFoxesClient {
    private static final ThreeToneFluidTint LIQUID_HONEY_TINTS =
            new ThreeToneFluidTint(0xFFE58E14, 0xFFE8A327, 0xFFE9C574);
    private static final ThreeToneFluidRenderer LIQUID_HONEY_RENDERER = new ThreeToneFluidRenderer(
            LIQUID_HONEY_TINTS.layer("liquid_honey_still_dark", "liquid_honey_flow_dark", LIQUID_HONEY_TINTS.dark()),
            LIQUID_HONEY_TINTS.layer("liquid_honey_still_medium", "liquid_honey_flow_medium", LIQUID_HONEY_TINTS.medium()),
            LIQUID_HONEY_TINTS.layer("liquid_honey_still_light", "liquid_honey_flow_light", LIQUID_HONEY_TINTS.light())
    );
    private static final ThreeToneFluidTint LIQUID_CHOCOLATE_TINTS =
            new ThreeToneFluidTint(0xFF612A34, 0xFF9F4B3B, 0xFFE6855E);
    private static final ThreeToneFluidRenderer LIQUID_CHOCOLATE_RENDERER = new ThreeToneFluidRenderer(
            LIQUID_CHOCOLATE_TINTS.layer("liquid_chocolate_still_dark", "liquid_chocolate_flow_dark", LIQUID_CHOCOLATE_TINTS.dark()),
            LIQUID_CHOCOLATE_TINTS.layer("liquid_chocolate_still_medium", "liquid_chocolate_flow_medium", LIQUID_CHOCOLATE_TINTS.medium()),
            LIQUID_CHOCOLATE_TINTS.layer("liquid_chocolate_still_light", "liquid_chocolate_flow_light", LIQUID_CHOCOLATE_TINTS.light())
    );

    public ProductiveFoxesClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::registerItemColors);
        modEventBus.addListener(ProductiveFoxesClient::onClientSetup);

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ProductiveFoxesEntities.BRAIXEN.get(), BraixenRenderer::new);
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_overlay");
            }

            @Override
            public int getTintColor() {
                return 0xFFA50700;
            }
        }, ProductiveFoxesFluids.SWEET_BERRY_PULP_TYPE.get());

        registerThreeToneFluid(
                event,
                ProductiveFoxesFluids.LIQUID_HONEY_TYPE.get(),
                ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_honey_still"),
                ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_honey_flow"),
                LIQUID_HONEY_TINTS,
                LIQUID_HONEY_RENDERER
        );
        registerThreeToneFluid(
                event,
                ProductiveFoxesFluids.LIQUID_CHOCOLATE_TYPE.get(),
                ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_chocolate_still"),
                ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_chocolate_flow"),
                LIQUID_CHOCOLATE_TINTS,
                LIQUID_CHOCOLATE_RENDERER
        );
    }

    private void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), ProductiveFoxesItems.SWEET_BERRY_PULP_BUCKET.get());
        event.register(new DynamicFluidContainerModel.Colors(), ProductiveFoxesItems.LIQUID_HONEY_BUCKET.get());
        event.register(new DynamicFluidContainerModel.Colors(), ProductiveFoxesItems.LIQUID_CHOCOLATE_BUCKET.get());
    }

    private static void registerThreeToneFluid(
            RegisterClientExtensionsEvent event,
            net.neoforged.neoforge.fluids.FluidType fluidType,
            ResourceLocation fallbackStillTexture,
            ResourceLocation fallbackFlowingTexture,
            ThreeToneFluidTint tints,
            ThreeToneFluidRenderer renderer
    ) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return fallbackStillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return fallbackFlowingTexture;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }

            @Override
            public Vector3f modifyFogColor(
                    Camera camera,
                    float partialTick,
                    ClientLevel level,
                    int renderDistance,
                    float darkenWorldAmount,
                    Vector3f fluidFogColor
            ) {
                return tints.mediumFogColor();
            }

            @Override
            public boolean renderFluid(
                    FluidState fluidState,
                    BlockAndTintGetter getter,
                    BlockPos pos,
                    VertexConsumer vertexConsumer,
                    BlockState blockState
            ) {
                return renderer.render(fluidState, getter, pos, vertexConsumer, blockState);
            }
        }, fluidType);
    }

    private record ThreeToneFluidTint(int dark, int medium, int light) {
        private Vector3f mediumFogColor() {
            return new Vector3f(red(medium), green(medium), blue(medium));
        }

        private ThreeToneFluidRenderer.Layer layer(String stillTexture, String flowingTexture, int tint) {
            return new ThreeToneFluidRenderer.Layer(
                    ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/" + stillTexture),
                    ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/" + flowingTexture),
                    tint
            );
        }

        private static float red(int argb) {
            return ((argb >> 16) & 0xFF) / 255.0F;
        }

        private static float green(int argb) {
            return ((argb >> 8) & 0xFF) / 255.0F;
        }

        private static float blue(int argb) {
            return (argb & 0xFF) / 255.0F;
        }
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ProductiveFoxes.LOGGER.info("HELLO FROM CLIENT SETUP");
        ProductiveFoxes.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.LIQUID_HONEY.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.FLOWING_LIQUID_HONEY.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.LIQUID_CHOCOLATE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.FLOWING_LIQUID_CHOCOLATE.get(), RenderType.translucent());
        });
    }
}
