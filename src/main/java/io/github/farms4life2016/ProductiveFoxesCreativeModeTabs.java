package io.github.farms4life2016;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProductiveFoxesCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ProductiveFoxes.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PRODUCTIVE_FOXES_TAB =
            CREATIVE_MODE_TABS.register(ProductiveFoxes.MODID, () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.productivefoxes"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ProductiveFoxesItems.BRAIXEN_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ProductiveFoxesItems.WASHED_SWEET_BERRIES.get());
                        output.accept(ProductiveFoxesItems.SWEET_BERRY_PULP_BUCKET.get());
                        output.accept(ProductiveFoxesItems.LIQUID_HONEY_BUCKET.get());
                        output.accept(ProductiveFoxesItems.BRAIXEN_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private ProductiveFoxesCreativeModeTabs() {
    }
}
