# Fluid Registration Notes

Current checkpoint:

- Branch when written: `1.21.1alpha/jam-production-experiments`
- Commit to inspect or branch from: `06b5e26014f9aacbc5ca7ff56b8d67396e212dd2`
- Useful commands:

```bash
git show --stat 06b5e26014f9aacbc5ca7ff56b8d67396e212dd2
git switch --detach 06b5e26014f9aacbc5ca7ff56b8d67396e212dd2
git switch -c inspect-fluid-poc 06b5e26014f9aacbc5ca7ff56b8d67396e212dd2
```

This doc describes the current POC wiring. If the class names move later, keep the same shape: common registration for the fluid/block/bucket, client registration for textures/tint/rendering, JSON assets for the block particle and dynamic bucket model.

## Current files to compare against

- `src/main/java/io/github/farms4life2016/ProductiveFoxesFluids.java`
- `src/main/java/io/github/farms4life2016/ProductiveFoxesBlocks.java`
- `src/main/java/io/github/farms4life2016/ProductiveFoxesItems.java`
- `src/main/java/io/github/farms4life2016/ProductiveFoxesCreativeModeTabs.java`
- `src/main/java/io/github/farms4life2016/ProductiveFoxesClient.java`
- `src/main/java/io/github/farms4life2016/client/ThreeToneFluidRenderer.java`
- `src/main/resources/assets/productivefoxes/models/item/*_bucket.json`
- `src/main/resources/assets/productivefoxes/blockstates/*.json`
- `src/main/resources/assets/productivefoxes/models/block/*.json`
- `src/main/resources/assets/productivefoxes/textures/block/*`
- `src/main/resources/assets/productivefoxes/lang/en_us.json`

## Common registration

Every fluid currently has:

- one `FluidType`
- one source fluid
- one flowing fluid
- one `LiquidBlock`
- one `BucketItem`
- one creative tab entry
- lang keys
- one bucket model using NeoForge's dynamic fluid-container loader
- one blockstate and one minimal block model for particles

Example, using placeholder names:

```java
// ProductiveFoxesFluids.java
public static final DeferredHolder<FluidType, FluidType> COOL_SYRUP_TYPE =
        FLUID_TYPES.register("cool_syrup", () -> new FluidType(baseProperties()
                .density(1400)
                .viscosity(2400)));

public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> COOL_SYRUP =
        FLUIDS.register("cool_syrup", () -> new BaseFlowingFluid.Source(coolSyrupProperties()));

public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_COOL_SYRUP =
        FLUIDS.register("flowing_cool_syrup",
                () -> new BaseFlowingFluid.Flowing(coolSyrupProperties()));

private static BaseFlowingFluid.Properties coolSyrupProperties() {
    return new BaseFlowingFluid.Properties(COOL_SYRUP_TYPE, COOL_SYRUP, FLOWING_COOL_SYRUP)
            .bucket(ProductiveFoxesItems.COOL_SYRUP_BUCKET)
            .block(ProductiveFoxesBlocks.COOL_SYRUP)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2)
            .tickRate(30)
            .explosionResistance(100.0F);
}
```

```java
// ProductiveFoxesBlocks.java
public static final DeferredBlock<LiquidBlock> COOL_SYRUP =
        BLOCKS.register("cool_syrup", () -> new LiquidBlock(ProductiveFoxesFluids.COOL_SYRUP.get(),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_YELLOW)
                        .replaceable()
                        .noCollission()
                        .strength(100.0F)
                        .noLootTable()
                        .liquid()
                        .pushReaction(PushReaction.DESTROY)));
```

```java
// ProductiveFoxesItems.java
public static final DeferredItem<BucketItem> COOL_SYRUP_BUCKET = ITEMS.registerItem(
        "cool_syrup_bucket",
        properties -> new BucketItem(ProductiveFoxesFluids.COOL_SYRUP.get(),
                properties.craftRemainder(Items.BUCKET).stacksTo(1))
);
```

```java
// ProductiveFoxesCreativeModeTabs.java
output.accept(ProductiveFoxesItems.COOL_SYRUP_BUCKET.get());
```

The main mod constructor must register fluids on the mod event bus:

```java
ProductiveFoxesBlocks.register(modEventBus);
ProductiveFoxesItems.register(modEventBus);
ProductiveFoxesFluids.register(modEventBus);
ProductiveFoxesCreativeModeTabs.register(modEventBus);
```

## Dynamic bucket model

Use this for all current bucket items. It is the Mekanism-style low-art path and avoids making custom bucket PNGs for every fluid.

```json
{
  "parent": "neoforge:item/bucket",
  "fluid": "productivefoxes:cool_syrup",
  "loader": "neoforge:fluid_container"
}
```

Register the item color handler client-side:

```java
// ProductiveFoxesClient.java
private void registerItemColors(RegisterColorHandlersEvent.Item event) {
    event.register(new DynamicFluidContainerModel.Colors(), ProductiveFoxesItems.COOL_SYRUP_BUCKET.get());
}
```

If the JEI preview is invisible and the bucket looks empty, check the fluid tint alpha first. Use ARGB, not RGB. Opaque red is `0xFFFF0000`, not `0xFF0000`.

## Coloured water

Current example: `sweet_berry_pulp`.

This is the simplest path: use vanilla water textures and tint them. No custom fluid renderer is needed.

```java
// ProductiveFoxesClient.java
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
```

Minimal block model:

```json
{
  "textures": {
    "particle": "minecraft:block/water_still"
  }
}
```

Use this pattern when the source liquid can look like tinted water.

## Coloured Create honey

Current example: `liquid_honey`.

Create's honey/chocolate have texture detail that does not look good as one flat tint. The current POC keeps three generated texture layers and renders them together with `ThreeToneFluidRenderer`.

Expected texture names:

```text
textures/block/liquid_honey_still.png
textures/block/liquid_honey_flow.png
textures/block/liquid_honey_still_dark.png
textures/block/liquid_honey_still_medium.png
textures/block/liquid_honey_still_light.png
textures/block/liquid_honey_flow_dark.png
textures/block/liquid_honey_flow_medium.png
textures/block/liquid_honey_flow_light.png
```

Client setup:

```java
private static final ThreeToneFluidTint LIQUID_HONEY_TINTS =
        new ThreeToneFluidTint(0xFFE58E14, 0xFFE8A327, 0xFFE9C574);

private static final ThreeToneFluidRenderer LIQUID_HONEY_RENDERER = new ThreeToneFluidRenderer(
        LIQUID_HONEY_TINTS.layer("liquid_honey_still_dark", "liquid_honey_flow_dark", LIQUID_HONEY_TINTS.dark()),
        LIQUID_HONEY_TINTS.layer("liquid_honey_still_medium", "liquid_honey_flow_medium", LIQUID_HONEY_TINTS.medium()),
        LIQUID_HONEY_TINTS.layer("liquid_honey_still_light", "liquid_honey_flow_light", LIQUID_HONEY_TINTS.light())
);

registerThreeToneFluid(
        event,
        ProductiveFoxesFluids.LIQUID_HONEY_TYPE.get(),
        ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_honey_still"),
        ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_honey_flow"),
        LIQUID_HONEY_TINTS,
        LIQUID_HONEY_RENDERER
);
```

Render layer:

```java
ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.LIQUID_HONEY.get(), RenderType.translucent());
ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.FLOWING_LIQUID_HONEY.get(), RenderType.translucent());
```

## Coloured Create chocolate

Current example: `liquid_chocolate`.

This is the same pattern as honey with different IDs, texture names, and tints.

```java
private static final ThreeToneFluidTint LIQUID_CHOCOLATE_TINTS =
        new ThreeToneFluidTint(0xFF612A34, 0xFF9F4B3B, 0xFFE6855E);

private static final ThreeToneFluidRenderer LIQUID_CHOCOLATE_RENDERER = new ThreeToneFluidRenderer(
        LIQUID_CHOCOLATE_TINTS.layer("liquid_chocolate_still_dark", "liquid_chocolate_flow_dark", LIQUID_CHOCOLATE_TINTS.dark()),
        LIQUID_CHOCOLATE_TINTS.layer("liquid_chocolate_still_medium", "liquid_chocolate_flow_medium", LIQUID_CHOCOLATE_TINTS.medium()),
        LIQUID_CHOCOLATE_TINTS.layer("liquid_chocolate_still_light", "liquid_chocolate_flow_light", LIQUID_CHOCOLATE_TINTS.light())
);

registerThreeToneFluid(
        event,
        ProductiveFoxesFluids.LIQUID_CHOCOLATE_TYPE.get(),
        ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_chocolate_still"),
        ResourceLocation.fromNamespaceAndPath(ProductiveFoxes.MODID, "block/liquid_chocolate_flow"),
        LIQUID_CHOCOLATE_TINTS,
        LIQUID_CHOCOLATE_RENDERER
);
```

Render layer:

```java
ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.LIQUID_CHOCOLATE.get(), RenderType.translucent());
ItemBlockRenderTypes.setRenderLayer(ProductiveFoxesFluids.FLOWING_LIQUID_CHOCOLATE.get(), RenderType.translucent());
```

## Three-tone helper shape

The client helper currently expects ARGB colors and texture names under `assets/productivefoxes/textures/block`.

```java
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
}
```

The extension returns white tint because the renderer handles layer tinting:

```java
private static void registerThreeToneFluid(
        RegisterClientExtensionsEvent event,
        FluidType fluidType,
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
```

## Onboarding another coloured mod liquid

1. Confirm the source mod/license is usable for the file you want. MIT is easy. If the license is not MIT, add the right notice before copying assets.
2. Pick an ID, for example `cool_syrup`.
3. Add `COOL_SYRUP_TYPE`, `COOL_SYRUP`, `FLOWING_COOL_SYRUP`, and `coolSyrupProperties()` in the fluid registry.
4. Add `COOL_SYRUP` as a `LiquidBlock`.
5. Add `COOL_SYRUP_BUCKET` as a `BucketItem`.
6. Add the bucket to the creative tab.
7. Add lang keys for `block`, `fluid`, `fluid_type`, and `item`.
8. Add `blockstates/cool_syrup.json`.
9. Add `models/block/cool_syrup.json` with a particle texture.
10. Add `models/item/cool_syrup_bucket.json` using `neoforge:fluid_container`.
11. Choose the client render path:
    - tinted water: use vanilla water textures plus `getTintColor()`.
    - textured liquid: copy/source still/flow textures and use the three-tone renderer.
12. Register the bucket with `DynamicFluidContainerModel.Colors()`.
13. Run:

```bash
rtk ./gradlew compileJava
rtk ./gradlew processResources
```

For most future liquids, start with coloured water. Only use the three-tone renderer when one tint loses important texture detail.
