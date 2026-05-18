package io.github.farms4life2016;

import io.github.farms4life2016.vaporeon_port.Vaporeon;
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

    private ProductiveFoxesEntities() {
    }
}
