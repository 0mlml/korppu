package dev.mlml.korppu.misc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;

public enum KorppuRenderLayers {
    ;
    public static final RenderType LINES = RenderType.create("korppu:lines", RenderSetup.builder(KorppuShaderPipelines.DEPTH_TEST_LINES).bufferSize(1536).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
    public static final RenderType ESP_LINES = RenderType.create("korppu:esp_lines", RenderSetup.builder(KorppuShaderPipelines.ESP_LINES).bufferSize(1536).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
    public static final RenderType LINE_STRIP = RenderType.create("korppu:line_strip", RenderSetup.builder(KorppuShaderPipelines.DEPTH_TEST_LINE_STRIP).bufferSize(1536).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
    public static final RenderType ESP_LINE_STRIP = RenderType.create("korppu:esp_line_strip", RenderSetup.builder(KorppuShaderPipelines.ESP_LINE_STRIP).bufferSize(1536).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
    public static final RenderType QUADS = RenderType.create("korppu:quads", RenderSetup.builder(KorppuShaderPipelines.QUADS).bufferSize(1536).createRenderSetup());
    public static final RenderType ESP_QUADS = RenderType.create("korppu:esp_quads", RenderSetup.builder(KorppuShaderPipelines.ESP_QUADS).bufferSize(1536).createRenderSetup());
    public static final RenderType ESP_QUADS_NO_CULLING = RenderType.create("korppu:esp_quads_no_culling", RenderSetup.builder(KorppuShaderPipelines.ESP_QUADS_NO_CULLING).bufferSize(1536).createRenderSetup());

    // Pipeline accessors for KorppuRenderer (MeshBuilder/RenderPass path)
    public static RenderPipeline getLinesPipeline(boolean depthTest) {
        return depthTest ? KorppuShaderPipelines.DEPTH_TEST_LINES : KorppuShaderPipelines.ESP_LINES;
    }

    public static RenderPipeline getLineStripPipeline(boolean depthTest) {
        return depthTest ? KorppuShaderPipelines.DEPTH_TEST_LINE_STRIP : KorppuShaderPipelines.ESP_LINE_STRIP;
    }

    public static RenderPipeline getQuadsPipeline(boolean depthTest) {
        return depthTest ? KorppuShaderPipelines.QUADS : KorppuShaderPipelines.ESP_QUADS;
    }

    // Legacy RenderType accessors (kept for any code still using bufferSource)
    public static RenderType getQuads(boolean depthTest) {
        return depthTest ? QUADS : ESP_QUADS;
    }

    public static RenderType getLines(boolean depthTest) {
        return depthTest ? LINES : ESP_LINES;
    }

    public static RenderType getLineStrip(boolean depthTest) {
        return depthTest ? LINE_STRIP : ESP_LINE_STRIP;
    }
}
