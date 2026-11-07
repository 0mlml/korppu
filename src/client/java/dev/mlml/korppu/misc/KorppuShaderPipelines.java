package dev.mlml.korppu.misc;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public enum KorppuShaderPipelines {
    ;
    private static final DepthStencilState NO_DEPTH_TEST = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    private static final DepthStencilState LEQUAL_DEPTH_TEST = new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);

    // DEBUG_LINES = GL_LINES with line-smooth; POSITION_COLOR matches Meteor's proven pattern.
    private static RenderPipeline.Builder linesBuilder() {
        return RenderPipeline.builder()
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                .withVertexShader(Identifier.parse("minecraft:core/position_color"))
                .withFragmentShader(Identifier.parse("minecraft:core/position_color"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES);
    }

    public static final RenderPipeline DEPTH_TEST_LINES = RenderPipelines.register(
            linesBuilder().withLocation(Identifier.parse("korppu:pipeline/korppu_depth_test_lines")).build());
    public static final RenderPipeline ESP_LINES = RenderPipelines.register(
            linesBuilder().withLocation(Identifier.parse("korppu:pipeline/korppu_esp_lines")).withDepthStencilState(NO_DEPTH_TEST).build());
    public static final RenderPipeline DEPTH_TEST_LINE_STRIP = RenderPipelines.register(
            linesBuilder().withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
                    .withLocation(Identifier.parse("korppu:pipeline/korppu_depth_test_line_strip")).build());
    public static final RenderPipeline ESP_LINE_STRIP = RenderPipelines.register(
            linesBuilder().withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
                    .withLocation(Identifier.parse("korppu:pipeline/korppu_esp_line_strip")).withDepthStencilState(NO_DEPTH_TEST).build());
    public static final RenderPipeline QUADS = RenderPipelines.register(
            RenderPipeline.builder()
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                    .withVertexShader(Identifier.parse("minecraft:core/position_color"))
                    .withFragmentShader(Identifier.parse("minecraft:core/position_color"))
                    .withLocation(Identifier.parse("korppu:pipeline/korppu_quads"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(LEQUAL_DEPTH_TEST)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build());
    public static final RenderPipeline ESP_QUADS = RenderPipelines.register(
            RenderPipeline.builder()
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                    .withVertexShader(Identifier.parse("minecraft:core/position_color"))
                    .withFragmentShader(Identifier.parse("minecraft:core/position_color"))
                    .withLocation(Identifier.parse("korppu:pipeline/korppu_esp_quads"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(NO_DEPTH_TEST)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build());
    public static final RenderPipeline ESP_QUADS_NO_CULLING = RenderPipelines.register(
            RenderPipeline.builder()
                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                    .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                    .withVertexShader(Identifier.parse("minecraft:core/position_color"))
                    .withFragmentShader(Identifier.parse("minecraft:core/position_color"))
                    .withLocation(Identifier.parse("korppu:pipeline/korppu_esp_quads_no_culling"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withDepthStencilState(NO_DEPTH_TEST)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build());
}
