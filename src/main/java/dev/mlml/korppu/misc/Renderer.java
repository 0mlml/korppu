package dev.mlml.korppu.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.mlml.korppu.KorppuMod;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Renderer
{
    public static void setupRender()
    {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void endRender()
    {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static float[] getColor(Color c)
    {
        return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f};
    }

    public static class WRLDR
    {
        static final MatrixStack empty = new MatrixStack();
        static List<Renderable> renderStack = new ArrayList<>();

        static Vec3d transformVec3d(Vec3d in)
        {
            Camera camera = KorppuMod.mc.gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();
            return in.subtract(camPos);
        }

        public static void renderLine(MatrixStack matrices, Color color, Vec3d start, Vec3d end)
        {
            Matrix4f s = matrices.peek().getPositionMatrix();
            pushRenderable(new Renderable(start.add(end.subtract(start)).multiply(.5))
            {
                @Override
                void draw()
                {
                    genericAABBRender(
                            VertexFormat.DrawMode.DEBUG_LINES,
                            VertexFormats.POSITION_COLOR,
                            GameRenderer::getPositionColorProgram,
                            s,
                            start,
                            end.subtract(start),
                            color,
                            (buffer, x, y, z, x1, y1, z1, red, green, blue, alpha, matrix) ->
                            {
                                buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                            }
                    );
                }
            });
        }

        private static void genericAABBRender(VertexFormat.DrawMode mode, VertexFormat format, Supplier<ShaderProgram> shader, Matrix4f stack, Vec3d start, Vec3d dimensions, Color color,
                                              RenderAction action)
        {
            float red = color.getRed() / 255f;
            float green = color.getGreen() / 255f;
            float blue = color.getBlue() / 255f;
            float alpha = color.getAlpha() / 255f;
            Vec3d vec3d = transformVec3d(start);
            Vec3d end = vec3d.add(dimensions);
            float x1 = (float) vec3d.x;
            float y1 = (float) vec3d.y;
            float z1 = (float) vec3d.z;
            float x2 = (float) end.x;
            float y2 = (float) end.y;
            float z2 = (float) end.z;
            useBuffer(mode, format, shader, bufferBuilder -> action.run(bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, stack));
        }

        public static void renderOutline(MatrixStack stack, Color color, Vec3d start, Vec3d dimensions)
        {
            Matrix4f m = stack.peek().getPositionMatrix();
            pushRenderable(new Renderable(start.add(dimensions.multiply(.5)))
            {
                @Override
                void draw()
                {
                    genericAABBRender(
                            VertexFormat.DrawMode.DEBUG_LINES,
                            VertexFormats.POSITION_COLOR,
                            GameRenderer::getPositionColorProgram,
                            m,
                            start,
                            dimensions,
                            color,
                            (buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, matrix) ->
                            {
                                buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();

                                buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();

                                buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();

                                buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();

                                buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();

                                buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                                buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                            }
                    );
                }
            });
        }

        private static void useBuffer(VertexFormat.DrawMode mode, VertexFormat format, Supplier<ShaderProgram> shader, Consumer<BufferBuilder> runner)
        {
            Tessellator t = Tessellator.getInstance();
            BufferBuilder bb = t.getBuffer();

            bb.begin(mode, format);

            runner.accept(bb);

            setupRender();
            RenderSystem.setShader(shader);
            BufferRenderer.drawWithGlobalProgram(bb.end());
            endRender();
        }

        static void pushRenderable(Renderable rnd)
        {
            renderStack.add(rnd);
        }

        public static void renderActions()
        {
            Camera c = KorppuMod.mc.gameRenderer.getCamera();
            Vec3d cp = c.getPos();
            renderStack.stream().sorted(Comparator.comparingDouble(value -> -value.pos.distanceTo(cp))).forEach(Renderable::draw);
            renderStack.clear();
        }

        public static MatrixStack getEmptyMatrixStack()
        {
            empty.loadIdentity();
            return empty;
        }

        interface RenderAction
        {
            void run(BufferBuilder buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha, Matrix4f matrix);
        }

        @Getter
        static abstract class Renderable
        {
            Vec3d pos;

            public Renderable(Vec3d pos)
            {
                this.pos = pos;
            }

            abstract void draw();
        }
    }
}
