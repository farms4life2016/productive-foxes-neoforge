package io.github.farms4life2016;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ProductiveFoxesFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ProductiveFoxes.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, ProductiveFoxes.MODID);

    public static final DeferredHolder<FluidType, FluidType> SWEET_BERRY_PULP_TYPE =
            FLUID_TYPES.register("sweet_berry_pulp", () -> new FluidType(baseProperties()
                    .density(1200)
                    .viscosity(1800)));

    public static final DeferredHolder<FluidType, FluidType> LIQUID_HONEY_TYPE =
            FLUID_TYPES.register("liquid_honey", () -> new FluidType(baseProperties()
                    .density(1400)
                    .viscosity(2400)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SWEET_BERRY_PULP =
            FLUIDS.register("sweet_berry_pulp", () -> new BaseFlowingFluid.Source(sweetBerryPulpProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SWEET_BERRY_PULP =
            FLUIDS.register("flowing_sweet_berry_pulp",
                    () -> new BaseFlowingFluid.Flowing(sweetBerryPulpProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_HONEY =
            FLUIDS.register("liquid_honey", () -> new BaseFlowingFluid.Source(liquidHoneyProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LIQUID_HONEY =
            FLUIDS.register("flowing_liquid_honey",
                    () -> new BaseFlowingFluid.Flowing(liquidHoneyProperties()));

    private static FluidType.Properties baseProperties() {
        return FluidType.Properties.create()
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    private static BaseFlowingFluid.Properties sweetBerryPulpProperties() {
        return new BaseFlowingFluid.Properties(SWEET_BERRY_PULP_TYPE, SWEET_BERRY_PULP, FLOWING_SWEET_BERRY_PULP)
                .bucket(ProductiveFoxesItems.SWEET_BERRY_PULP_BUCKET)
                .block(ProductiveFoxesBlocks.SWEET_BERRY_PULP)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(25)
                .explosionResistance(100.0F);
    }

    private static BaseFlowingFluid.Properties liquidHoneyProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_HONEY_TYPE, LIQUID_HONEY, FLOWING_LIQUID_HONEY)
                .bucket(ProductiveFoxesItems.LIQUID_HONEY_BUCKET)
                .block(ProductiveFoxesBlocks.LIQUID_HONEY)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    private ProductiveFoxesFluids() {
    }
}
