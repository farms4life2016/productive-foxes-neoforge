/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Cobblemon source code: https://gitlab.com/cable-mc/cobblemon/-/tree/main?ref_type=heads
 *
 * Adapted for Productive Foxes from Cobblemon's Bedrock model stuff.
 * - https://gitlab.com/cable-mc/cobblemon/-/blob/main/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/TexturedModel.kt
 * - https://gitlab.com/cable-mc/cobblemon/-/tree/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/TexturedModel.kt
 * - https://gitlab.com/cable-mc/cobblemon/-/tree/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/bedrock/animation/BedrockAnimation.kt
 * - https://gitlab.com/cable-mc/cobblemon/-/tree/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/bedrock/animation/BedrockPoseAnimation.kt
 * - https://gitlab.com/cable-mc/cobblemon/-/tree/30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/bedrock/animation/BedrockAnimationAdapter.kt
 */

package io.github.farms4life2016.vixen_maid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BedrockGeoModel {
    private final ModelPart root;
    private final Map<String, ModelPart> partsByName;
    private final Map<ModelPart, PartPose> defaultPoses;
    private final Map<String, BedrockAnimation> animations;

    private BedrockGeoModel(ModelPart root, Map<String, ModelPart> partsByName, Map<String, BedrockAnimation> animations) {
        this.root = root;
        this.partsByName = partsByName;
        this.animations = animations;
        this.defaultPoses = new IdentityHashMap<>();
        for (ModelPart part : partsByName.values()) {
            this.defaultPoses.put(part, part.storePose());
        }
    }

    public static BedrockGeoModel load(ResourceLocation location) {
        return load(location, null);
    }

    public static BedrockGeoModel load(ResourceLocation location, ResourceLocation animationLocation) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                throw new IllegalStateException("Missing Bedrock geo resource: " + location);
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                BakedBedrockModel model = createModel(root);
                return new BedrockGeoModel(
                        model.root,
                        model.partsByName,
                        animationLocation != null ? loadAnimations(animationLocation) : Map.of()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Bedrock geo model " + location, exception);
        }
    }

    public void applyAnimation(String animationName, float animationSeconds) {
        this.resetPose();
        BedrockAnimation animation = this.animations.get(animationName);
        if (animation == null) {
            animation = this.animations.get("animation.braixen." + animationName);
        }
        if (animation != null) {
            animation.apply(this.partsByName, animationSeconds);
        }
    }

    public void lookAt(String partName, float yawDegrees, float pitchDegrees) {
        ModelPart part = this.partsByName.get(partName);
        if (part == null) {
            return;
        }
        part.yRot += radians(yawDegrees);
        part.xRot += radians(pitchDegrees);
    }

    public void setVisible(String partName, boolean visible) {
        ModelPart part = this.partsByName.get(partName);
        if (part != null) {
            part.visible = visible;
        }
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }

    private void resetPose() {
        for (Map.Entry<ModelPart, PartPose> entry : this.defaultPoses.entrySet()) {
            entry.getKey().loadPose(entry.getValue());
            entry.getKey().visible = true;
        }
    }

    private static BakedBedrockModel createModel(JsonObject root) {
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

        ModelPart rootPart = LayerDefinition.create(mesh, textureWidth, textureHeight).bakeRoot();
        Map<String, ModelPart> bakedParts = new HashMap<>();
        for (ModelBone bone : orderedBones) {
            ModelPart part = bone.parent == null
                    ? rootPart.getChild(bone.name)
                    : bakedParts.get(bone.parent).getChild(bone.name);
            bakedParts.put(bone.name, part);
        }

        return new BakedBedrockModel(rootPart, bakedParts);
    }

    private static Map<String, BedrockAnimation> loadAnimations(ResourceLocation location) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                throw new IllegalStateException("Missing Bedrock animation resource: " + location);
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animationsJson = root.getAsJsonObject("animations");
                if (animationsJson == null) {
                    return Map.of();
                }

                Map<String, BedrockAnimation> animations = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : animationsJson.entrySet()) {
                    animations.put(entry.getKey(), BedrockAnimation.read(entry.getValue().getAsJsonObject()));
                }
                return animations;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Bedrock animation " + location, exception);
        }
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

    private record BakedBedrockModel(ModelPart root, Map<String, ModelPart> partsByName) {
    }

    private record BedrockAnimation(Map<String, BoneAnimation> bones) {
        private static BedrockAnimation read(JsonObject json) {
            Map<String, BoneAnimation> bones = new HashMap<>();
            JsonObject bonesJson = json.getAsJsonObject("bones");
            if (bonesJson != null) {
                for (Map.Entry<String, JsonElement> entry : bonesJson.entrySet()) {
                    bones.put(entry.getKey(), BoneAnimation.read(entry.getValue().getAsJsonObject()));
                }
            }
            return new BedrockAnimation(bones);
        }

        private void apply(Map<String, ModelPart> partsByName, double animationSeconds) {
            for (Map.Entry<String, BoneAnimation> entry : this.bones.entrySet()) {
                ModelPart part = partsByName.get(entry.getKey());
                if (part != null) {
                    entry.getValue().apply(part, animationSeconds);
                }
            }
        }
    }

    private record BoneAnimation(VectorExpression position, VectorExpression rotation) {
        private static BoneAnimation read(JsonObject json) {
            return new BoneAnimation(
                    VectorExpression.read(json.get("position")),
                    VectorExpression.read(json.get("rotation"))
            );
        }

        private void apply(ModelPart part, double animationSeconds) {
            if (this.position != null) {
                double[] position = this.position.evaluate(animationSeconds);
                part.x += (float) position[0];
                part.y -= (float) position[1];
                part.z += (float) position[2];
            }

            if (this.rotation != null) {
                double[] rotation = this.rotation.evaluate(animationSeconds);
                part.xRot += radians((float) rotation[0]);
                part.yRot += radians((float) rotation[1]);
                part.zRot += radians((float) rotation[2]);
            }
        }
    }

    private record VectorExpression(ScalarExpression x, ScalarExpression y, ScalarExpression z) {
        private static VectorExpression read(JsonElement json) {
            if (json == null || !json.isJsonArray()) {
                return null;
            }

            JsonArray array = json.getAsJsonArray();
            return new VectorExpression(
                    ScalarExpression.read(array.get(0)),
                    ScalarExpression.read(array.get(1)),
                    ScalarExpression.read(array.get(2))
            );
        }

        private double[] evaluate(double animationSeconds) {
            return new double[]{
                    this.x.evaluate(animationSeconds),
                    this.y.evaluate(animationSeconds),
                    this.z.evaluate(animationSeconds)
            };
        }
    }

    private record ScalarExpression(String expression) {
        private static ScalarExpression read(JsonElement json) {
            return new ScalarExpression(json.getAsString());
        }

        private double evaluate(double animationSeconds) {
            return new ExpressionParser(this.expression, animationSeconds).parse();
        }
    }

    private static final class ExpressionParser {
        private final String expression;
        private final double animationSeconds;
        private int index;

        private ExpressionParser(String expression, double animationSeconds) {
            this.expression = expression.replace("query.", "q.");
            this.animationSeconds = animationSeconds;
        }

        private double parse() {
            double value = parseExpression();
            skipWhitespace();
            if (this.index != this.expression.length()) {
                throw new IllegalArgumentException("Unexpected token in animation expression: " + this.expression.substring(this.index));
            }
            return value;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parseFactor();
                } else if (match('/')) {
                    value /= parseFactor();
                } else {
                    return value;
                }
            }
        }

        private double parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return -parseFactor();
            }
            if (match('(')) {
                double value = parseExpression();
                expect(')');
                return value;
            }
            if (peekDigit()) {
                return parseNumber();
            }
            return parseIdentifier();
        }

        private double parseIdentifier() {
            String identifier = readIdentifier();
            if ("q.anim_time".equals(identifier)) {
                return this.animationSeconds;
            }
            if ("math.sin".equals(identifier)) {
                expect('(');
                double value = parseExpression();
                expect(')');
                return Math.sin(Math.toRadians(value));
            }
            if ("math.abs".equals(identifier)) {
                expect('(');
                double value = parseExpression();
                expect(')');
                return Math.abs(value);
            }
            if ("math.clamp".equals(identifier)) {
                expect('(');
                double value = parseExpression();
                expect(',');
                double min = parseExpression();
                expect(',');
                double max = parseExpression();
                expect(')');
                return Math.max(min, Math.min(max, value));
            }
            throw new IllegalArgumentException("Unknown animation expression identifier: " + identifier);
        }

        private String readIdentifier() {
            int start = this.index;
            while (this.index < this.expression.length()) {
                char c = this.expression.charAt(this.index);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                    this.index++;
                } else {
                    break;
                }
            }
            if (start == this.index) {
                throw new IllegalArgumentException("Expected identifier in animation expression: " + this.expression);
            }
            return this.expression.substring(start, this.index);
        }

        private double parseNumber() {
            int start = this.index;
            while (this.index < this.expression.length()) {
                char c = this.expression.charAt(this.index);
                if ((c >= '0' && c <= '9') || c == '.') {
                    this.index++;
                } else {
                    break;
                }
            }
            return Double.parseDouble(this.expression.substring(start, this.index));
        }

        private boolean peekDigit() {
            if (this.index >= this.expression.length()) {
                return false;
            }
            char c = this.expression.charAt(this.index);
            return (c >= '0' && c <= '9') || c == '.';
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (this.index < this.expression.length() && this.expression.charAt(this.index) == expected) {
                this.index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!match(expected)) {
                throw new IllegalArgumentException("Expected '" + expected + "' in animation expression: " + this.expression);
            }
        }

        private void skipWhitespace() {
            while (this.index < this.expression.length() && Character.isWhitespace(this.expression.charAt(this.index))) {
                this.index++;
            }
        }
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
