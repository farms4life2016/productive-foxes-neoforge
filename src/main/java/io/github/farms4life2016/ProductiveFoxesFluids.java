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

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SWEET_BERRY_PULP =
            FLUIDS.register("sweet_berry_pulp", () -> new BaseFlowingFluid.Source(sweetBerryPulpProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SWEET_BERRY_PULP =
            FLUIDS.register("flowing_sweet_berry_pulp",
                    () -> new BaseFlowingFluid.Flowing(sweetBerryPulpProperties()));

    // ponytail: lemon juice is water-thin, so no density/viscosity override on the FluidType.
    public static final DeferredHolder<FluidType, FluidType> SOUR_BERRY_JUICE_TYPE =
            FLUID_TYPES.register("sour_berry_juice", () -> new FluidType(baseProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOUR_BERRY_JUICE =
            FLUIDS.register("sour_berry_juice", () -> new BaseFlowingFluid.Source(sourBerryJuiceProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SOUR_BERRY_JUICE =
            FLUIDS.register("flowing_sour_berry_juice",
                    () -> new BaseFlowingFluid.Flowing(sourBerryJuiceProperties()));

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

    private static BaseFlowingFluid.Properties sourBerryJuiceProperties() {
        return new BaseFlowingFluid.Properties(SOUR_BERRY_JUICE_TYPE, SOUR_BERRY_JUICE, FLOWING_SOUR_BERRY_JUICE)
                .bucket(ProductiveFoxesItems.SOUR_BERRY_JUICE_BUCKET)
                .block(ProductiveFoxesBlocks.SOUR_BERRY_JUICE)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(25)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    private ProductiveFoxesFluids() {
    }
}
