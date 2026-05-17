/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Cobblemon source code: https://gitlab.com/cable-mc/cobblemon/-/tree/main?ref_type=heads
 *
 * Adapted for Productive Foxes from Cobblemon's Bedrock GEO model baking
 * logic in TexturedModel.kt (https://gitlab.com/cable-mc/cobblemon/-/blob/main/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/TexturedModel.kt).
 */

package io.github.farms4life2016.vaporeon_port;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BedrockGeoModel {
    private final ModelPart root;

    private BedrockGeoModel(ModelPart root) {
        this.root = root;
    }

    public static BedrockGeoModel load(ResourceLocation location) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                throw new IllegalStateException("Missing Bedrock geo resource: " + location);
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return new BedrockGeoModel(createLayer(root).bakeRoot());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Bedrock geo model " + location, exception);
        }
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }

    private static LayerDefinition createLayer(JsonObject root) {
        JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        int textureWidth = description.get("texture_width").getAsInt();
        int textureHeight = description.get("texture_height").getAsInt();

        MeshDefinition mesh = new MeshDefinition();
        Map<String, ModelBone> bones = new HashMap<>();
        Map<String, PartDefinition> parts = new HashMap<>();
        List<ModelBone> orderedBones = new ArrayList<>();

        for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
            ModelBone bone = ModelBone.read(boneElement.getAsJsonObject());
            orderedBones.add(bone);
            bones.put(bone.name, bone);
        }

        for (ModelBone bone : orderedBones) {
            PartDefinition parentPart = bone.parent != null && parts.containsKey(bone.parent)
                    ? parts.get(bone.parent)
                    : mesh.getRoot();
            ModelBone parentBone = bone.parent != null ? bones.get(bone.parent) : null;

            CubeListBuilder boneBuilder = CubeListBuilder.create();
            List<SubPart> subParts = new ArrayList<>();

            for (ModelCube cube : bone.cubes) {
                CubeListBuilder targetBuilder = cube.hasRotation() ? CubeListBuilder.create() : boneBuilder;
                float[] pivot = cube.pivot != null ? cube.pivot : bone.pivot;

                if (cube.uv != null) {
                    targetBuilder.texOffs(cube.uv[0], cube.uv[1]);
                }
                if (cube.mirror) {
                    targetBuilder.mirror();
                }

                targetBuilder.addBox(
                        cube.origin[0] - pivot[0],
                        -(cube.origin[1] - pivot[1] + cube.size[1]),
                        cube.origin[2] - pivot[2],
                        cube.size[0],
                        cube.size[1],
                        cube.size[2],
                        new CubeDeformation(cube.inflate)
                );

                if (cube.mirror) {
                    targetBuilder.mirror(false);
                }
                if (cube.hasRotation()) {
                    subParts.add(new SubPart(targetBuilder, cubePose(bone, pivot, cube.rotation)));
                }
            }

            PartDefinition part = parentPart.addOrReplaceChild(bone.name, boneBuilder, bonePose(bone, parentBone));
            parts.put(bone.name, part);

            int index = 0;
            for (SubPart subPart : subParts) {
                part.addOrReplaceChild("%" + bone.name + "%" + index++, subPart.builder, subPart.pose);
            }
        }

        return LayerDefinition.create(mesh, textureWidth, textureHeight);
    }

    private static PartPose bonePose(ModelBone bone, ModelBone parent) {
        if (parent == null) {
            return PartPose.offset(0.0F, 0.0F, 0.0F);
        }

        float x = -(parent.pivot[0] - bone.pivot[0]);
        float y = parent.pivot[1] - bone.pivot[1];
        float z = -(parent.pivot[2] - bone.pivot[2]);
        if (bone.rotation == null) {
            return PartPose.offset(x, y, z);
        }
        return PartPose.offsetAndRotation(
                x,
                y,
                z,
                radians(bone.rotation[0]),
                radians(bone.rotation[1]),
                radians(bone.rotation[2])
        );
    }

    private static PartPose cubePose(ModelBone bone, float[] cubePivot, float[] rotation) {
        return PartPose.offsetAndRotation(
                -(bone.pivot[0] - cubePivot[0]),
                bone.pivot[1] - cubePivot[1],
                -(bone.pivot[2] - cubePivot[2]),
                radians(rotation[0]),
                radians(rotation[1]),
                radians(rotation[2])
        );
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static float[] readFloatArray(JsonArray array, int size, float defaultValue) {
        float[] values = new float[size];
        for (int index = 0; index < size; index++) {
            values[index] = array != null && index < array.size() ? array.get(index).getAsFloat() : defaultValue;
        }
        return values;
    }

    private static int[] readIntArray(JsonArray array, int size) {
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = array.get(index).getAsInt();
        }
        return values;
    }

    private record SubPart(CubeListBuilder builder, PartPose pose) {
    }

    private static final class ModelBone {
        private final String name;
        private final String parent;
        private final float[] pivot;
        private final float[] rotation;
        private final List<ModelCube> cubes;

        private ModelBone(String name, String parent, float[] pivot, float[] rotation, List<ModelCube> cubes) {
            this.name = name;
            this.parent = parent;
            this.pivot = pivot;
            this.rotation = rotation;
            this.cubes = cubes;
        }

        private static ModelBone read(JsonObject json) {
            String name = json.get("name").getAsString();
            String parent = json.has("parent") ? json.get("parent").getAsString() : null;
            float[] pivot = readFloatArray(json.getAsJsonArray("pivot"), 3, 0.0F);
            float[] rotation = json.has("rotation") ? readFloatArray(json.getAsJsonArray("rotation"), 3, 0.0F) : null;
            List<ModelCube> cubes = new ArrayList<>();

            JsonArray cubeArray = json.getAsJsonArray("cubes");
            if (cubeArray != null) {
                for (JsonElement cubeElement : cubeArray) {
                    cubes.add(ModelCube.read(cubeElement.getAsJsonObject()));
                }
            }

            return new ModelBone(name, parent, pivot, rotation, cubes);
        }
    }

    private static final class ModelCube {
        private final float[] origin;
        private final float[] size;
        private final float[] pivot;
        private final float[] rotation;
        private final int[] uv;
        private final float inflate;
        private final boolean mirror;

        private ModelCube(float[] origin, float[] size, float[] pivot, float[] rotation, int[] uv, float inflate, boolean mirror) {
            this.origin = origin;
            this.size = size;
            this.pivot = pivot;
            this.rotation = rotation;
            this.uv = uv;
            this.inflate = inflate;
            this.mirror = mirror;
        }

        private static ModelCube read(JsonObject json) {
            float[] origin = readFloatArray(json.getAsJsonArray("origin"), 3, 0.0F);
            float[] size = readFloatArray(json.getAsJsonArray("size"), 3, 0.0F);
            float[] pivot = json.has("pivot") ? readFloatArray(json.getAsJsonArray("pivot"), 3, 0.0F) : null;
            float[] rotation = json.has("rotation") ? readFloatArray(json.getAsJsonArray("rotation"), 3, 0.0F) : null;
            int[] uv = json.has("uv") && json.get("uv").isJsonArray() ? readIntArray(json.getAsJsonArray("uv"), 2) : null;
            float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;
            boolean mirror = json.has("mirror") && json.get("mirror").getAsBoolean();
            return new ModelCube(origin, size, pivot, rotation, uv, inflate, mirror);
        }

        private boolean hasRotation() {
            return this.rotation != null;
        }
    }
}
