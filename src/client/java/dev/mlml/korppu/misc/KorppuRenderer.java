package dev.mlml.korppu.misc;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Minimal MeshBuilder/RenderPass-based world renderer following Meteor's pattern.
 * Builds vertices into a ByteBuffer, then submits via RenderPass with MC's
 * DynamicTransforms and Globals UBOs auto-bound.
 */
public class KorppuRenderer {
    private static final Vector4f WHITE = new Vector4f(1, 1, 1, 1);
    private static final Vector3f ZERO_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final RenderPipeline pipeline;
    private final int vertexSize;

    private ByteBuffer vertices;
    private long verticesStart, verticesPtr;

    private ByteBuffer indices;
    private long indicesPtr;

    private int vertexI;
    private int indexCount;

    private double cameraX, cameraY, cameraZ;

    public KorppuRenderer(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        this.vertexSize = pipeline.getVertexFormat().getVertexSize();
    }

    public void begin() {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        cameraX = cam.x;
        cameraY = cam.y;
        cameraZ = cam.z;

        if (vertices == null) allocate(256, 512);

        verticesPtr = verticesStart;
        vertexI = 0;
        indexCount = 0;
    }

    // POSITION_COLOR layout: Position(12) + Color(4) = 16 bytes.
    // X/Z are camera-relative; Y is raw world Y (the -cameraY translation is applied in render()).
    public int vertex(double x, double y, double z, int color) {
        ensureVertexCapacity();
        long p = verticesPtr;
        memPutFloat(p,     (float)(x - cameraX));
        memPutFloat(p + 4, (float) y);
        memPutFloat(p + 8, (float)(z - cameraZ));
        memPutByte(p + 12, (byte)((color >> 16) & 0xFF));
        memPutByte(p + 13, (byte)((color >> 8)  & 0xFF));
        memPutByte(p + 14, (byte)(color          & 0xFF));
        memPutByte(p + 15, (byte)((color >> 24)  & 0xFF));
        verticesPtr += vertexSize;
        return vertexI++;
    }

    // Convenience aliases so callers don't need to change if they used the old names.
    public int lineVertex(double x, double y, double z, int color) {
        return vertex(x, y, z, color);
    }

    public int quadVertex(double x, double y, double z, int color) {
        return vertex(x, y, z, color);
    }

    public void addLine(int i1, int i2) {
        ensureIndexCapacity(2);
        long p = indicesPtr + indexCount * 4L;
        memPutInt(p,     i1);
        memPutInt(p + 4, i2);
        indexCount += 2;
    }

    public void addQuad(int i1, int i2, int i3, int i4) {
        ensureIndexCapacity(6);
        long p = indicesPtr + indexCount * 4L;
        memPutInt(p,      i1);
        memPutInt(p + 4,  i2);
        memPutInt(p + 8,  i3);
        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);
        indexCount += 6;
    }

    public void render() {
        if (indexCount == 0) return;

        vertices.limit((int)(verticesPtr - verticesStart));
        GpuBuffer vbo = pipeline.getVertexFormat().uploadImmediateVertexBuffer(vertices);

        indices.limit(indexCount * 4);
        GpuBuffer ibo = pipeline.getVertexFormat().uploadImmediateIndexBuffer(indices);

        // Apply camera Y offset to match Meteor's applyCameraPos() pattern.
        // Vertices store (x - camX, worldY, z - camZ); this translation completes the camera-relative transform.
        RenderSystem.getModelViewStack().pushMatrix().translate(0, (float) -cameraY, 0);

        // Write DynamicTransforms with the model-view matrix (now includes -camY translation)
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewStack());
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, WHITE, ZERO_OFFSET, IDENTITY);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(
                        () -> "Korppu Renderer",
                        Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                        java.util.OptionalInt.empty(),
                        Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
                        java.util.OptionalDouble.empty())) {

            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);

            pass.setVertexBuffer(0, vbo);
            pass.setIndexBuffer(ibo, VertexFormat.IndexType.INT);
            pass.drawIndexed(0, 0, indexCount, 1);
        }

        RenderSystem.getModelViewStack().popMatrix();
        // vbo and ibo are owned by the VertexFormat's immediateDrawVertexBuffer/indexBuffer —
        // do NOT close them here; the format manages their lifetime across frames.
    }

    private void ensureVertexCapacity() {
        if (vertices == null) { allocate(256, 512); return; }
        if ((vertexI + 1) * vertexSize >= vertices.capacity()) {
            int used = (int)(verticesPtr - verticesStart);
            ByteBuffer next = BufferUtils.createByteBuffer(vertices.capacity() * 2);
            memCopy(verticesStart, memAddress0(next), used);
            vertices = next;
            verticesStart = memAddress0(vertices);
            verticesPtr = verticesStart + used;
        }
    }

    private void ensureIndexCapacity(int needed) {
        if (indices == null) { allocate(256, 512); return; }
        if ((indexCount + needed) * 4 >= indices.capacity()) {
            ByteBuffer next = BufferUtils.createByteBuffer(indices.capacity() * 2);
            memCopy(memAddress0(indices), memAddress0(next), indexCount * 4L);
            indices = next;
            indicesPtr = memAddress0(indices);
        }
    }

    private void allocate(int vertCount, int idxCount) {
        vertices = BufferUtils.createByteBuffer(vertexSize * vertCount);
        verticesStart = verticesPtr = memAddress0(vertices);
        indices = BufferUtils.createByteBuffer(idxCount * 4);
        indicesPtr = memAddress0(indices);
    }
}
