package io.github.farms4life2016;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.farms4life2016.custom_berries.SourBerryBushBlock;

public final class ProductiveFoxesBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ProductiveFoxes.MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK =
            BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

    public static final DeferredBlock<SourBerryBushBlock> SOUR_BERRY_BUSH =
            BLOCKS.register("sour_berry_bush", () -> new SourBerryBushBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));

    public static final DeferredBlock<LiquidBlock> SWEET_BERRY_PULP =
            BLOCKS.register("sweet_berry_pulp", () -> new LiquidBlock(ProductiveFoxesFluids.SWEET_BERRY_PULP.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .noLootTable()
                            .liquid()
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<LiquidBlock> SOUR_BERRY_JUICE =
            BLOCKS.register("sour_berry_juice", () -> new LiquidBlock(ProductiveFoxesFluids.SOUR_BERRY_JUICE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_YELLOW)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .noLootTable()
                            .liquid()
                            .pushReaction(PushReaction.DESTROY)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private ProductiveFoxesBlocks() {
    }
}
