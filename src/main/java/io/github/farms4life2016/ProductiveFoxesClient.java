package io.github.farms4life2016;

import io.github.farms4life2016.vaporeon_port.VaporeonRenderer;
import io.github.farms4life2016.vixen_maid.BraixenRenderer;
import io.github.farms4life2016.vixen_maid.DelphoxRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ProductiveFoxes.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ProductiveFoxes.MODID, value = Dist.CLIENT)
public class ProductiveFoxesClient {
    public ProductiveFoxesClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerEntityRenderers);

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ProductiveFoxesEntities.VAPOREON.get(), VaporeonRenderer::new);
        event.registerEntityRenderer(ProductiveFoxesEntities.BRAIXEN.get(), BraixenRenderer::new);
        event.registerEntityRenderer(ProductiveFoxesEntities.DELPHOX.get(), DelphoxRenderer::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ProductiveFoxes.LOGGER.info("HELLO FROM CLIENT SETUP");
        ProductiveFoxes.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
