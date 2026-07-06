package io.github.farms4life2016.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;

public final class ThreeToneFluidRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;
    private static final float TOP_FACE_OFFSET = 0.001F;
    private static final float SIDE_FACE_OFFSET = 0.001F;

    private final Layer[] layers;

    public ThreeToneFluidRenderer(Layer dark, Layer medium, Layer light) {
        this.layers = new Layer[] {dark, medium, light};
    }

    public boolean render(
            FluidState fluidState,
            BlockAndTintGetter level,
            BlockPos pos,
            VertexConsumer buffer,
            BlockState blockState
    ) {
        Geometry geometry = Geometry.create(level, pos, fluidState, blockState);
        if (!geometry.hasVisibleFaces()) {
            return true;
        }

        for (Layer layer : layers) {
            TextureAtlasSprite still = FluidSpriteCache.getSprite(layer.stillTexture());
            TextureAtlasSprite flowing = FluidSpriteCache.getSprite(layer.flowingTexture());
            renderLayer(level, pos, fluidState, buffer, geometry, still, flowing, layer.argbTint());
        }
        return true;
    }

    private static void renderLayer(
            BlockAndTintGetter level,
            BlockPos pos,
            FluidState fluidState,
            VertexConsumer buffer,
            Geometry geometry,
            TextureAtlasSprite stillSprite,
            TextureAtlasSprite flowingSprite,
            int argbTint
    ) {
        float alpha = (float)(argbTint >> 24 & 0xFF) / 255.0F;
        float red = (float)(argbTint >> 16 & 0xFF) / 255.0F;
        float green = (float)(argbTint >> 8 & 0xFF) / 255.0F;
        float blue = (float)(argbTint & 0xFF) / 255.0F;

        float downShade = level.getShade(Direction.DOWN, true);
        float upShade = level.getShade(Direction.UP, true);
        float northShade = level.getShade(Direction.NORTH, true);
        float westShade = level.getShade(Direction.WEST, true);

        float x = (float)(pos.getX() & 15);
        float y = (float)(pos.getY() & 15);
        float z = (float)(pos.getZ() & 15);
        float bottom = geometry.renderDown() ? TOP_FACE_OFFSET : 0.0F;

        if (geometry.renderUp() && !isFaceOccludedByNeighbor(level, pos, Direction.UP, geometry.minTopHeight(), geometry.upBlock())) {
            renderTopFace(level, pos, fluidState, buffer, geometry, stillSprite, flowingSprite, x, y, z,
                    upShade * red, upShade * green, upShade * blue, alpha);
        }

        if (geometry.renderDown()) {
            int light = getLightColor(level, pos.below());
            float u0 = stillSprite.getU0();
            float u1 = stillSprite.getU1();
            float v0 = stillSprite.getV0();
            float v1 = stillSprite.getV1();
            float bottomY = y + bottom;
            vertex(buffer, x, bottomY, z + 1.0F, downShade * red, downShade * green, downShade * blue, alpha, u0, v1, light);
            vertex(buffer, x, bottomY, z, downShade * red, downShade * green, downShade * blue, alpha, u0, v0, light);
            vertex(buffer, x + 1.0F, bottomY, z, downShade * red, downShade * green, downShade * blue, alpha, u1, v0, light);
            vertex(buffer, x + 1.0F, bottomY, z + 1.0F, downShade * red, downShade * green, downShade * blue, alpha, u1, v1, light);
        }

        int light = getLightColor(level, pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            renderSideFace(level, pos, fluidState, buffer, geometry, flowingSprite, direction, x, y, z, bottom,
                    upShade, northShade, westShade, red, green, blue, alpha, light);
        }
    }

    private static void renderTopFace(
            BlockAndTintGetter level,
            BlockPos pos,
            FluidState fluidState,
            VertexConsumer buffer,
            Geometry geometry,
            TextureAtlasSprite stillSprite,
            TextureAtlasSprite flowingSprite,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float northEastHeight = geometry.northEastHeight() - TOP_FACE_OFFSET;
        float northWestHeight = geometry.northWestHeight() - TOP_FACE_OFFSET;
        float southEastHeight = geometry.southEastHeight() - TOP_FACE_OFFSET;
        float southWestHeight = geometry.southWestHeight() - TOP_FACE_OFFSET;
        Vec3 flow = fluidState.getFlow(level, pos);

        float u0;
        float u1;
        float u2;
        float u3;
        float v0;
        float v1;
        float v2;
        float v3;
        TextureAtlasSprite shrinkSprite;
        if (flow.x == 0.0 && flow.z == 0.0) {
            shrinkSprite = stillSprite;
            u0 = stillSprite.getU(0.0F);
            v0 = stillSprite.getV(0.0F);
            u1 = u0;
            v1 = stillSprite.getV(1.0F);
            u2 = stillSprite.getU(1.0F);
            v2 = v1;
            u3 = u2;
            v3 = v0;
        } else {
            shrinkSprite = flowingSprite;
            float angle = (float)Mth.atan2(flow.z, flow.x) - (float)(Math.PI / 2);
            float sin = Mth.sin(angle) * 0.25F;
            float cos = Mth.cos(angle) * 0.25F;
            u0 = flowingSprite.getU(0.5F + (-cos - sin));
            v0 = flowingSprite.getV(0.5F + -cos + sin);
            u1 = flowingSprite.getU(0.5F + -cos + sin);
            v1 = flowingSprite.getV(0.5F + cos + sin);
            u2 = flowingSprite.getU(0.5F + cos + sin);
            v2 = flowingSprite.getV(0.5F + (cos - sin));
            u3 = flowingSprite.getU(0.5F + (cos - sin));
            v3 = flowingSprite.getV(0.5F + (-cos - sin));
        }

        float centerU = (u0 + u1 + u2 + u3) / 4.0F;
        float centerV = (v0 + v1 + v2 + v3) / 4.0F;
        float shrink = shrinkSprite.uvShrinkRatio();
        u0 = Mth.lerp(shrink, u0, centerU);
        u1 = Mth.lerp(shrink, u1, centerU);
        u2 = Mth.lerp(shrink, u2, centerU);
        u3 = Mth.lerp(shrink, u3, centerU);
        v0 = Mth.lerp(shrink, v0, centerV);
        v1 = Mth.lerp(shrink, v1, centerV);
        v2 = Mth.lerp(shrink, v2, centerV);
        v3 = Mth.lerp(shrink, v3, centerV);

        int light = getLightColor(level, pos);
        vertex(buffer, x, y + northWestHeight, z, red, green, blue, alpha, u0, v0, light);
        vertex(buffer, x, y + southWestHeight, z + 1.0F, red, green, blue, alpha, u1, v1, light);
        vertex(buffer, x + 1.0F, y + southEastHeight, z + 1.0F, red, green, blue, alpha, u2, v2, light);
        vertex(buffer, x + 1.0F, y + northEastHeight, z, red, green, blue, alpha, u3, v3, light);
        if (fluidState.shouldRenderBackwardUpFace(level, pos.above())) {
            vertex(buffer, x, y + northWestHeight, z, red, green, blue, alpha, u0, v0, light);
            vertex(buffer, x + 1.0F, y + northEastHeight, z, red, green, blue, alpha, u3, v3, light);
            vertex(buffer, x + 1.0F, y + southEastHeight, z + 1.0F, red, green, blue, alpha, u2, v2, light);
            vertex(buffer, x, y + southWestHeight, z + 1.0F, red, green, blue, alpha, u1, v1, light);
        }
    }

    private static void renderSideFace(
            BlockAndTintGetter level,
            BlockPos pos,
            FluidState fluidState,
            VertexConsumer buffer,
            Geometry geometry,
            TextureAtlasSprite flowingSprite,
            Direction direction,
            float x,
            float y,
            float z,
            float bottom,
            float upShade,
            float northShade,
            float westShade,
            float red,
            float green,
            float blue,
            float alpha,
            int light
    ) {
        SideGeometry side = geometry.side(direction, x, z);
        if (!side.render()
                || isFaceOccludedByNeighbor(level, pos, direction, Math.max(side.leftHeight(), side.rightHeight()),
                level.getBlockState(pos.relative(direction)))) {
            return;
        }

        float u0 = flowingSprite.getU(0.0F);
        float u1 = flowingSprite.getU(0.5F);
        float v0 = flowingSprite.getV((1.0F - side.leftHeight()) * 0.5F);
        float v1 = flowingSprite.getV((1.0F - side.rightHeight()) * 0.5F);
        float v2 = flowingSprite.getV(0.5F);
        float sideShade = direction.getAxis() == Direction.Axis.Z ? northShade : westShade;
        float shadedRed = upShade * sideShade * red;
        float shadedGreen = upShade * sideShade * green;
        float shadedBlue = upShade * sideShade * blue;

        vertex(buffer, side.leftX(), y + side.leftHeight(), side.leftZ(), shadedRed, shadedGreen, shadedBlue, alpha, u0, v0, light);
        vertex(buffer, side.rightX(), y + side.rightHeight(), side.rightZ(), shadedRed, shadedGreen, shadedBlue, alpha, u1, v1, light);
        vertex(buffer, side.rightX(), y + bottom, side.rightZ(), shadedRed, shadedGreen, shadedBlue, alpha, u1, v2, light);
        vertex(buffer, side.leftX(), y + bottom, side.leftZ(), shadedRed, shadedGreen, shadedBlue, alpha, u0, v2, light);

        vertex(buffer, side.leftX(), y + bottom, side.leftZ(), shadedRed, shadedGreen, shadedBlue, alpha, u0, v2, light);
        vertex(buffer, side.rightX(), y + bottom, side.rightZ(), shadedRed, shadedGreen, shadedBlue, alpha, u1, v2, light);
        vertex(buffer, side.rightX(), y + side.rightHeight(), side.rightZ(), shadedRed, shadedGreen, shadedBlue, alpha, u1, v1, light);
        vertex(buffer, side.leftX(), y + side.leftHeight(), side.leftZ(), shadedRed, shadedGreen, shadedBlue, alpha, u0, v0, light);
    }

    private static float calculateAverageHeight(
            BlockAndTintGetter level,
            Fluid fluid,
            float currentHeight,
            float height1,
            float height2,
            BlockPos pos
    ) {
        if (height2 >= 1.0F || height1 >= 1.0F) {
            return 1.0F;
        }

        float[] output = new float[2];
        if (height2 > 0.0F || height1 > 0.0F) {
            float height = getHeight(level, fluid, pos);
            if (height >= 1.0F) {
                return 1.0F;
            }

            addWeightedHeight(output, height);
        }

        addWeightedHeight(output, currentHeight);
        addWeightedHeight(output, height2);
        addWeightedHeight(output, height1);
        return output[0] / output[1];
    }

    private static void addWeightedHeight(float[] output, float height) {
        if (height >= 0.8F) {
            output[0] += height * 10.0F;
            output[1] += 10.0F;
        } else if (height >= 0.0F) {
            output[0] += height;
            output[1]++;
        }
    }

    private static float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return getHeight(level, fluid, pos, blockState, blockState.getFluidState());
    }

    private static float getHeight(
            BlockAndTintGetter level,
            Fluid fluid,
            BlockPos pos,
            BlockState blockState,
            FluidState fluidState
    ) {
        if (fluid.isSame(fluidState.getType())) {
            BlockState above = level.getBlockState(pos.above());
            return fluid.isSame(above.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        }
        return !blockState.isSolid() ? 0.0F : -1.0F;
    }

    private static boolean isNeighborStateHidingOverlay(FluidState selfState, BlockState otherState, Direction neighborFace) {
        return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
    }

    private static boolean isFaceOccludedByState(
            BlockGetter level,
            Direction face,
            float height,
            BlockPos pos,
            BlockState state
    ) {
        if (!state.canOcclude()) {
            return false;
        }

        VoxelShape fluidShape = Shapes.box(0.0, 0.0, 0.0, 1.0, height, 1.0);
        VoxelShape stateShape = state.getOcclusionShape(level, pos);
        return Shapes.blockOccudes(fluidShape, stateShape, face);
    }

    private static boolean isFaceOccludedByNeighbor(
            BlockGetter level,
            BlockPos pos,
            Direction side,
            float height,
            BlockState blockState
    ) {
        return isFaceOccludedByState(level, side, height, pos.relative(side), blockState);
    }

    private static boolean isFaceOccludedBySelf(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return isFaceOccludedByState(level, face.getOpposite(), 1.0F, pos, state);
    }

    private static boolean shouldRenderFace(
            BlockAndTintGetter level,
            BlockPos pos,
            FluidState fluidState,
            BlockState selfState,
            Direction direction,
            BlockState otherState
    ) {
        return !isFaceOccludedBySelf(level, pos, selfState, direction)
                && !isNeighborStateHidingOverlay(fluidState, otherState, direction.getOpposite());
    }

    private static void vertex(
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int packedLight
    ) {
        buffer.addVertex(x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static int getLightColor(BlockAndTintGetter level, BlockPos pos) {
        int current = LevelRenderer.getLightColor(level, pos);
        int above = LevelRenderer.getLightColor(level, pos.above());
        int blockLight = current & 0xFF;
        int aboveBlockLight = above & 0xFF;
        int skyLight = current >> 16 & 0xFF;
        int aboveSkyLight = above >> 16 & 0xFF;
        return (Math.max(blockLight, aboveBlockLight)) | (Math.max(skyLight, aboveSkyLight)) << 16;
    }

    public record Layer(ResourceLocation stillTexture, ResourceLocation flowingTexture, int argbTint) {
    }

    private record Geometry(
            BlockState downBlock,
            BlockState upBlock,
            boolean renderUp,
            boolean renderDown,
            boolean renderNorth,
            boolean renderSouth,
            boolean renderWest,
            boolean renderEast,
            float northEastHeight,
            float northWestHeight,
            float southEastHeight,
            float southWestHeight
    ) {
        private static Geometry create(BlockAndTintGetter level, BlockPos pos, FluidState fluidState, BlockState blockState) {
            BlockState downBlock = level.getBlockState(pos.relative(Direction.DOWN));
            BlockState upBlock = level.getBlockState(pos.relative(Direction.UP));
            BlockState northBlock = level.getBlockState(pos.relative(Direction.NORTH));
            BlockState southBlock = level.getBlockState(pos.relative(Direction.SOUTH));
            BlockState westBlock = level.getBlockState(pos.relative(Direction.WEST));
            BlockState eastBlock = level.getBlockState(pos.relative(Direction.EAST));

            boolean renderUp = !isNeighborStateHidingOverlay(fluidState, upBlock, Direction.DOWN);
            boolean renderDown = shouldRenderFace(level, pos, fluidState, blockState, Direction.DOWN, downBlock)
                    && !isFaceOccludedByNeighbor(level, pos, Direction.DOWN, MAX_FLUID_HEIGHT, downBlock);
            boolean renderNorth = shouldRenderFace(level, pos, fluidState, blockState, Direction.NORTH, northBlock);
            boolean renderSouth = shouldRenderFace(level, pos, fluidState, blockState, Direction.SOUTH, southBlock);
            boolean renderWest = shouldRenderFace(level, pos, fluidState, blockState, Direction.WEST, westBlock);
            boolean renderEast = shouldRenderFace(level, pos, fluidState, blockState, Direction.EAST, eastBlock);

            Fluid fluid = fluidState.getType();
            float ownHeight = getHeight(level, fluid, pos, blockState, fluidState);
            float northEastHeight;
            float northWestHeight;
            float southEastHeight;
            float southWestHeight;
            if (ownHeight >= 1.0F) {
                northEastHeight = 1.0F;
                northWestHeight = 1.0F;
                southEastHeight = 1.0F;
                southWestHeight = 1.0F;
            } else {
                float northHeight = getHeight(level, fluid, pos.north(), northBlock, northBlock.getFluidState());
                float southHeight = getHeight(level, fluid, pos.south(), southBlock, southBlock.getFluidState());
                float eastHeight = getHeight(level, fluid, pos.east(), eastBlock, eastBlock.getFluidState());
                float westHeight = getHeight(level, fluid, pos.west(), westBlock, westBlock.getFluidState());
                northEastHeight = calculateAverageHeight(level, fluid, ownHeight, northHeight, eastHeight,
                        pos.relative(Direction.NORTH).relative(Direction.EAST));
                northWestHeight = calculateAverageHeight(level, fluid, ownHeight, northHeight, westHeight,
                        pos.relative(Direction.NORTH).relative(Direction.WEST));
                southEastHeight = calculateAverageHeight(level, fluid, ownHeight, southHeight, eastHeight,
                        pos.relative(Direction.SOUTH).relative(Direction.EAST));
                southWestHeight = calculateAverageHeight(level, fluid, ownHeight, southHeight, westHeight,
                        pos.relative(Direction.SOUTH).relative(Direction.WEST));
            }

            return new Geometry(downBlock, upBlock, renderUp, renderDown, renderNorth, renderSouth, renderWest,
                    renderEast, northEastHeight, northWestHeight, southEastHeight, southWestHeight);
        }

        private boolean hasVisibleFaces() {
            return renderUp || renderDown || renderNorth || renderSouth || renderWest || renderEast;
        }

        private float minTopHeight() {
            return Math.min(Math.min(northWestHeight, southWestHeight), Math.min(southEastHeight, northEastHeight));
        }

        private SideGeometry side(Direction direction, float x, float z) {
            return switch (direction) {
                case NORTH -> new SideGeometry(northWestHeight, northEastHeight, x, x + 1.0F,
                        z + SIDE_FACE_OFFSET, z + SIDE_FACE_OFFSET, renderNorth);
                case SOUTH -> new SideGeometry(southEastHeight, southWestHeight, x + 1.0F, x,
                        z + 1.0F - SIDE_FACE_OFFSET, z + 1.0F - SIDE_FACE_OFFSET, renderSouth);
                case WEST -> new SideGeometry(southWestHeight, northWestHeight, x + SIDE_FACE_OFFSET,
                        x + SIDE_FACE_OFFSET, z + 1.0F, z, renderWest);
                case EAST -> new SideGeometry(northEastHeight, southEastHeight, x + 1.0F - SIDE_FACE_OFFSET,
                        x + 1.0F - SIDE_FACE_OFFSET, z, z + 1.0F, renderEast);
                default -> throw new IllegalArgumentException("Expected horizontal direction, got " + direction);
            };
        }
    }

    private record SideGeometry(
            float leftHeight,
            float rightHeight,
            float leftX,
            float rightX,
            float leftZ,
            float rightZ,
            boolean render
    ) {
    }
}
