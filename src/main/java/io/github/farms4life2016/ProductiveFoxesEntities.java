package io.github.farms4life2016;

import io.github.farms4life2016.vaporeon_port.Vaporeon;
import io.github.farms4life2016.vixen_maid.Braixen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProductiveFoxesEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ProductiveFoxes.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Vaporeon>> VAPOREON =
            ENTITY_TYPES.register("vaporeon", () -> EntityType.Builder
                    .of(Vaporeon::new, MobCategory.CREATURE)
                    .sized(0.72F, 0.92F)
                    .clientTrackingRange(10)
                    .build(ProductiveFoxes.MODID + ":vaporeon"));

    public static final DeferredHolder<EntityType<?>, EntityType<Braixen>> BRAIXEN =
            ENTITY_TYPES.register("braixen", () -> EntityType.Builder
                    .of(Braixen::new, MobCategory.CREATURE)
                    .sized(1F, 2.25F)
                    .eyeHeight(1.9125F)
                    .clientTrackingRange(10)
                    .build(ProductiveFoxes.MODID + ":braixen"));

    private ProductiveFoxesEntities() {
    }
}
