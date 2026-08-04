package io.github.farms4life2016;

import net.minecraft.world.food.Foods;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProductiveFoxesItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ProductiveFoxes.MODID);

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("example_block", ProductiveFoxesBlocks.EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem(
            "example_item",
            new Item.Properties().food(new FoodProperties.Builder()
                    .alwaysEdible()
                    .nutrition(1)
                    .saturationModifier(2f)
                    .build())
    );

    public static final DeferredItem<Item> WASHED_SWEET_BERRIES = ITEMS.registerSimpleItem(
            "washed_sweet_berries",
            new Item.Properties().food(Foods.SWEET_BERRIES)
    );

    // Right-clicking a dirt-like block with this places a sour berry bush; eating behaves like sweet berries.
    public static final DeferredItem<ItemNameBlockItem> SOUR_BERRIES = ITEMS.registerItem(
            "sour_berries",
            properties -> new ItemNameBlockItem(ProductiveFoxesBlocks.SOUR_BERRY_BUSH.get(), properties.food(Foods.SWEET_BERRIES))
    );

    public static final DeferredItem<BucketItem> SWEET_BERRY_PULP_BUCKET = ITEMS.registerItem(
            "sweet_berry_pulp_bucket",
            properties -> new BucketItem(ProductiveFoxesFluids.SWEET_BERRY_PULP.get(),
                    properties.craftRemainder(Items.BUCKET).stacksTo(1))
    );

    public static final DeferredItem<DeferredSpawnEggItem> BRAIXEN_SPAWN_EGG = ITEMS.registerItem(
            "braixen_spawn_egg",
            properties -> new DeferredSpawnEggItem(ProductiveFoxesEntities.BRAIXEN, 0xddc16c, 0xd75a39, properties)
    );

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private ProductiveFoxesItems() {
    }
}
