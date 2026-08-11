package io.github.farms4life2016;

import io.github.farms4life2016.vixen_maid.BraixenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ProductiveFoxes.MODID, dist = Dist.CLIENT)
public class ProductiveFoxesClient {
    // ponytail: tint multiply can only attenuate channels, so lemon-yellow (full R+G, low B)
    // warms the reused vanilla textures into a yellow-green bush with orange berries. Tweak this
    // single constant to recolour both the bush and the fruit item at once.
    private static final int LEMON_TINT = 0xFFFF66;

    // ponytail: tint multiply on the bluish water texture yields a yellow-green juice; raise B for a paler lemon.
    private static final int LEMON_JUICE_TINT = 0xFFFFF44F;

    public ProductiveFoxesClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::registerBlockColors);
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
                return LEMON_JUICE_TINT;
            }
        }, ProductiveFoxesFluids.SOUR_BERRY_JUICE_TYPE.get());

    }

    private void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), ProductiveFoxesItems.SWEET_BERRY_PULP_BUCKET.get());
        event.register(new DynamicFluidContainerModel.Colors(), ProductiveFoxesItems.SOUR_BERRY_JUICE_BUCKET.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? LEMON_TINT : -1, ProductiveFoxesItems.SOUR_BERRIES.get());
    }

    private void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? LEMON_TINT : -1, ProductiveFoxesBlocks.SOUR_BERRY_BUSH.get());
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ProductiveFoxes.LOGGER.info("HELLO FROM CLIENT SETUP");
        ProductiveFoxes.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
