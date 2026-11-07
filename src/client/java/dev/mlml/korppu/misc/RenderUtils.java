package dev.mlml.korppu.misc;

import java.util.List;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import net.minecraft.client.CameraType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkAccess;

public enum RenderUtils
{
	;

	public static void applyRegionalRenderOffset(PoseStack matrixStack)
	{
		applyRegionalRenderOffset(matrixStack, getCameraRegion());
	}

	public static void applyRegionalRenderOffset(PoseStack matrixStack,
		ChunkAccess chunk)
	{
		applyRegionalRenderOffset(matrixStack, RegionPos.of(chunk.getPos()));
	}

	public static void applyRegionalRenderOffset(PoseStack matrixStack,
		RegionPos region)
	{
		Vec3 offset = region.toVec3d().subtract(getCameraPos());
		matrixStack.translate(offset.x, offset.y, offset.z);
	}

	public static void applyRenderOffset(PoseStack matrixStack)
	{
		Vec3 camPos = getCameraPos();
		matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
	}

	public static Vec3 getCameraPos()
	{
		Camera camera = KorppuMod.mc.gameRenderer.getMainCamera();
		if(camera == null)
			return Vec3.ZERO;

		return camera.position();
	}

	public static BlockPos getCameraBlockPos()
	{
		Camera camera = KorppuMod.mc.gameRenderer.getMainCamera();
		if(camera == null)
			return BlockPos.ZERO;

		return camera.blockPosition();
	}

	public static RegionPos getCameraRegion()
	{
		return RegionPos.of(getCameraBlockPos());
	}

	public static float[] getRainbowColor()
	{
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float pi = (float)Math.PI;

		float[] rainbow = new float[3];
		rainbow[0] = 0.5F + 0.5F * Mth.sin(x * pi);
		rainbow[1] = 0.5F + 0.5F * Mth.sin((x + 4F / 3F) * pi);
		rainbow[2] = 0.5F + 0.5F * Mth.sin((x + 8F / 3F) * pi);
		return rainbow;
	}

	public static int toIntColor(float[] rgb, float opacity)
	{
		return (int)(Mth.clamp(opacity, 0, 1) * 255) << 24
			| (int)(Mth.clamp(rgb[0], 0, 1) * 255) << 16
			| (int)(Mth.clamp(rgb[1], 0, 1) * 255) << 8
			| (int)(Mth.clamp(rgb[2], 0, 1) * 255);
	}

	// ── Line drawing ──────────────────────────────────────────────────────────

	public static void drawLine(PoseStack matrices, Vec3 start, Vec3 end,
		int color, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		addLine(r, start.x, start.y, start.z, end.x, end.y, end.z, color);
		r.render();
	}

	private static Vec3 getTracerOrigin(float partialTicks)
	{
		Vec3 start = RotationUtils.getClientLookVec(partialTicks).scale(10);
		if(KorppuMod.mc.options
			.getCameraType() == CameraType.THIRD_PERSON_FRONT)
			start = start.reverse();
		return start;
	}

	public static void drawTracer(PoseStack matrices, float partialTicks,
		Vec3 end, int color, boolean depthTest)
	{
		Vec3 start = getTracerOrigin(partialTicks);
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		addLine(r, start.x, start.y, start.z, end.x, end.y, end.z, color);
		r.render();
	}

	public static void drawTracers(PoseStack matrices, float partialTicks,
		List<Vec3> ends, int color, boolean depthTest)
	{
		Vec3 start = getTracerOrigin(partialTicks);
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		for(Vec3 end : ends)
			addLine(r, start.x, start.y, start.z, end.x, end.y, end.z, color);
		r.render();
	}

	public static void drawTracers(PoseStack matrices, float partialTicks,
		List<ColoredPoint> ends, boolean depthTest)
	{
		Vec3 start = getTracerOrigin(partialTicks);
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		for(ColoredPoint end : ends)
			addLine(r, start.x, start.y, start.z, end.point().x, end.point().y, end.point().z, end.color());
		r.render();
	}

	private static void addLine(KorppuRenderer r,
		double x1, double y1, double z1,
		double x2, double y2, double z2, int color)
	{
		int a = r.lineVertex(x1, y1, z1, color);
		int b = r.lineVertex(x2, y2, z2, color);
		r.addLine(a, b);
	}

	public static void drawCurvedLine(PoseStack matrices, List<Vec3> points,
		int color, boolean depthTest)
	{
		if(points.size() < 2) return;
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLineStripPipeline(depthTest));
		r.begin();
		for(int i = 1; i < points.size(); i++)
		{
			Vec3 prev = points.get(i - 1);
			Vec3 curr = points.get(i);
			addLine(r, prev.x, prev.y, prev.z, curr.x, curr.y, curr.z, color);
		}
		r.render();
	}

	// ── Box drawing ───────────────────────────────────────────────────────────

	public static void drawSolidBox(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getQuadsPipeline(depthTest));
		r.begin();
		addSolidBox(r, box, color);
		r.render();
	}

	public static void drawSolidBoxes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getQuadsPipeline(depthTest));
		r.begin();
		for(AABB box : boxes) addSolidBox(r, box, color);
		r.render();
	}

	public static void drawSolidBoxes(PoseStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getQuadsPipeline(depthTest));
		r.begin();
		for(ColoredBox box : boxes) addSolidBox(r, box.box(), box.color());
		r.render();
	}

	public static void drawOutlinedBox(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		addOutlinedBox(r, box, color);
		r.render();
	}

	public static void drawOutlinedBoxes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		for(AABB box : boxes) addOutlinedBox(r, box, color);
		r.render();
	}

	public static void drawOutlinedBoxes(PoseStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		for(ColoredBox box : boxes) addOutlinedBox(r, box.box(), box.color());
		r.render();
	}

	public static void drawCrossBox(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		addCrossBox(r, box, color);
		r.render();
	}

	public static void drawCrossBoxes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		for(AABB box : boxes) addCrossBox(r, box, color);
		r.render();
	}

	public static void drawCrossBoxes(PoseStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		for(ColoredBox box : boxes) addCrossBox(r, box.box(), box.color());
		r.render();
	}

	public static void drawNode(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		KorppuRenderer r = new KorppuRenderer(KorppuRenderLayers.getLinesPipeline(depthTest));
		r.begin();
		addNode(r, box, color);
		r.render();
	}

	// ── Private geometry builders ─────────────────────────────────────────────

	private static void addSolidBox(KorppuRenderer r, AABB box, int color)
	{
		double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
		double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;

		// bottom
		int v0 = r.quadVertex(x1, y1, z1, color);
		int v1 = r.quadVertex(x2, y1, z1, color);
		int v2 = r.quadVertex(x2, y1, z2, color);
		int v3 = r.quadVertex(x1, y1, z2, color);
		r.addQuad(v0, v1, v2, v3);

		// top
		int v4 = r.quadVertex(x1, y2, z1, color);
		int v5 = r.quadVertex(x1, y2, z2, color);
		int v6 = r.quadVertex(x2, y2, z2, color);
		int v7 = r.quadVertex(x2, y2, z1, color);
		r.addQuad(v4, v5, v6, v7);

		// north
		int v8  = r.quadVertex(x1, y1, z1, color);
		int v9  = r.quadVertex(x1, y2, z1, color);
		int v10 = r.quadVertex(x2, y2, z1, color);
		int v11 = r.quadVertex(x2, y1, z1, color);
		r.addQuad(v8, v9, v10, v11);

		// east
		int v12 = r.quadVertex(x2, y1, z1, color);
		int v13 = r.quadVertex(x2, y2, z1, color);
		int v14 = r.quadVertex(x2, y2, z2, color);
		int v15 = r.quadVertex(x2, y1, z2, color);
		r.addQuad(v12, v13, v14, v15);

		// south
		int v16 = r.quadVertex(x1, y1, z2, color);
		int v17 = r.quadVertex(x2, y1, z2, color);
		int v18 = r.quadVertex(x2, y2, z2, color);
		int v19 = r.quadVertex(x1, y2, z2, color);
		r.addQuad(v16, v17, v18, v19);

		// west
		int v20 = r.quadVertex(x1, y1, z1, color);
		int v21 = r.quadVertex(x1, y1, z2, color);
		int v22 = r.quadVertex(x1, y2, z2, color);
		int v23 = r.quadVertex(x1, y2, z1, color);
		r.addQuad(v20, v21, v22, v23);
	}

	private static void addOutlinedBox(KorppuRenderer r, AABB box, int color)
	{
		double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
		double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;

		// bottom edges
		addBoxLine(r, x1,y1,z1, x2,y1,z1, color);
		addBoxLine(r, x1,y1,z1, x1,y1,z2, color);
		addBoxLine(r, x2,y1,z1, x2,y1,z2, color);
		addBoxLine(r, x1,y1,z2, x2,y1,z2, color);
		// top edges
		addBoxLine(r, x1,y2,z1, x2,y2,z1, color);
		addBoxLine(r, x1,y2,z1, x1,y2,z2, color);
		addBoxLine(r, x2,y2,z1, x2,y2,z2, color);
		addBoxLine(r, x1,y2,z2, x2,y2,z2, color);
		// vertical edges
		addBoxLine(r, x1,y1,z1, x1,y2,z1, color);
		addBoxLine(r, x2,y1,z1, x2,y2,z1, color);
		addBoxLine(r, x1,y1,z2, x1,y2,z2, color);
		addBoxLine(r, x2,y1,z2, x2,y2,z2, color);
	}

	private static void addBoxLine(KorppuRenderer r,
		double x1, double y1, double z1,
		double x2, double y2, double z2, int color)
	{
		int a = r.lineVertex(x1, y1, z1, color);
		int b = r.lineVertex(x2, y2, z2, color);
		r.addLine(a, b);
	}

	private static void addCrossBox(KorppuRenderer r, AABB box, int color)
	{
		double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
		double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;

		addLine(r, x1,y1,z1, x2,y2,z1, color);
		addLine(r, x2,y1,z1, x1,y2,z1, color);
		addLine(r, x2,y1,z1, x2,y2,z2, color);
		addLine(r, x2,y1,z2, x2,y2,z1, color);
		addLine(r, x2,y1,z2, x1,y2,z2, color);
		addLine(r, x1,y1,z2, x2,y2,z2, color);
		addLine(r, x1,y1,z2, x1,y2,z1, color);
		addLine(r, x1,y1,z1, x1,y2,z2, color);
		addLine(r, x1,y2,z2, x2,y2,z1, color);
		addLine(r, x2,y2,z2, x1,y2,z1, color);
		addLine(r, x1,y1,z1, x2,y1,z2, color);
		addLine(r, x2,y1,z1, x1,y1,z2, color);
	}

	private static void addNode(KorppuRenderer r, AABB box, int color)
	{
		double x1 = box.minX, y1 = box.minY, z1 = box.minZ;
		double x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
		double x3 = (x1+x2)/2, y3 = (y1+y2)/2, z3 = (z1+z2)/2;

		addLine(r, x3,y3,z2, x1,y3,z3, color);
		addLine(r, x1,y3,z3, x3,y3,z1, color);
		addLine(r, x3,y3,z1, x2,y3,z3, color);
		addLine(r, x2,y3,z3, x3,y3,z2, color);
		addLine(r, x3,y2,z3, x2,y3,z3, color);
		addLine(r, x3,y2,z3, x1,y3,z3, color);
		addLine(r, x3,y2,z3, x3,y3,z1, color);
		addLine(r, x3,y2,z3, x3,y3,z2, color);
		addLine(r, x3,y1,z3, x2,y3,z3, color);
		addLine(r, x3,y1,z3, x1,y3,z3, color);
		addLine(r, x3,y1,z3, x3,y3,z1, color);
		addLine(r, x3,y1,z3, x3,y3,z2, color);
	}

	// ── 3D text (still uses bufferSource — text rendering not broken) ──────────

	public static void drawText3d(PoseStack matrices, String text, double x, double y, double z, int color) {
		net.minecraft.client.gui.Font tr = KorppuMod.mc.font;
		MultiBufferSource.BufferSource vcp = KorppuMod.mc.renderBuffers().bufferSource();

		matrices.pushPose();
		matrices.translate(x, y, z);
		matrices.mulPose(KorppuMod.mc.gameRenderer.getMainCamera().rotation());
		matrices.scale(-0.025f, -0.025f, 0.025f);

		org.joml.Matrix4f matrix = matrices.last().pose();
		float tx = -tr.width(text) / 2f;
		tr.drawInBatch(text, tx, 0, color, false, matrix, vcp,
				net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x40000000,
				net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
		vcp.endBatch();

		matrices.popPose();
	}

	// ── 2D rendering (unchanged — uses GuiGraphicsExtractor, not broken) ──────

	public static void drawItem(GuiGraphicsExtractor context, ItemStack stack, int x,
                                int y, boolean large)
	{
		Matrix3x2fStack matrixStack = context.pose();

		matrixStack.pushMatrix();
		matrixStack.translate(x, y);
		if(large)
			matrixStack.scale(1.5F, 1.5F);
		else
			matrixStack.scale(0.75F, 0.75F);

		ItemStack renderStack = stack.isEmpty() || stack.getItem() == null
			? new ItemStack(Blocks.GRASS_BLOCK) : stack;

		context.item(renderStack, 0, 0);

		matrixStack.popMatrix();

		if(stack.isEmpty())
		{
			context.guiRenderState.nextStratum();
			matrixStack.pushMatrix();
			matrixStack.translate(x, y);
			if(large)
				matrixStack.scale(2, 2);

			Font tr = KorppuMod.mc.font;
			context.text(tr, "?", 3, 2, 0xFF00FF, true);

			matrixStack.popMatrix();
		}
	}

	public static void fill2D(GuiGraphicsExtractor context, float x1, float y1, float x2,
		float y2, int color)
	{
		int scale = KorppuMod.mc.getWindow().getGuiScale();
		int xs1 = (int)(x1 * scale);
		int ys1 = (int)(y1 * scale);
		int xs2 = (int)(x2 * scale);
		int ys2 = (int)(y2 * scale);

		context.pose().pushMatrix();
		context.pose().scale(1F / scale);
		context.fill(xs1, ys1, xs2, ys2, color);
		context.pose().popMatrix();
	}

	public static void fillQuads2D(GuiGraphicsExtractor context, float[][] vertices,
		int color)
	{
		Matrix3x2f pose = new Matrix3x2f(context.pose());
		ScreenRectangle scissor = context.scissorStack.peek();

		for(int i = 0; i < vertices.length - 3; i += 4)
		{
			if(i + 3 >= vertices.length)
				break;

			float x1 = vertices[i][0];
			float y1 = vertices[i][1];
			float x2 = vertices[i + 1][0];
			float y2 = vertices[i + 1][1];
			float x3 = vertices[i + 2][0];
			float y3 = vertices[i + 2][1];
			float x4 = vertices[i + 3][0];
			float y4 = vertices[i + 3][1];

			context.guiRenderState.addGuiElement(new CustomQuadRenderState(pose, x1,
				y1, x2, y2, x3, y3, x4, y4, color, scissor));
		}
	}

	public static void fillTriangle2D(GuiGraphicsExtractor context, float[][] vertices,
		int color)
	{
		Matrix3x2f pose = new Matrix3x2f(context.pose());
		ScreenRectangle scissor = context.scissorStack.peek();

		for(int i = 0; i < vertices.length - 2; i += 3)
		{
			if(i + 2 >= vertices.length)
				break;

			float x1 = vertices[i][0];
			float y1 = vertices[i][1];
			float x2 = vertices[i + 1][0];
			float y2 = vertices[i + 1][1];
			float x3 = vertices[i + 2][0];
			float y3 = vertices[i + 2][1];

			context.guiRenderState.addGuiElement(new CustomQuadRenderState(pose, x1,
				y1, x2, y2, x3, y3, x3, y3, color, scissor));
		}
	}

	public static void drawLine2D(GuiGraphicsExtractor context, float x1, float y1,
		float x2, float y2, int color)
	{
		int scale = KorppuMod.mc.getWindow().getGuiScale();
		float x = x1 * scale;
		float y = y1 * scale;
		float w = (x2 - x1) * scale;
		float h = (y2 - y1) * scale;
		float angle = (float)Mth.atan2(h, w);
		int length = Math.round(Mth.sqrt(w * w + h * h));

		context.pose().pushMatrix();
		context.pose().scale(1F / scale);
		context.pose().translate(x, y);
		context.pose().rotate(angle);
		context.pose().translate(-0.5F, -0.5F);
		context.horizontalLine(0, length - 1, 0, color);
		context.pose().popMatrix();
	}

	public static void drawBorder2D(GuiGraphicsExtractor context, float x1, float y1,
		float x2, float y2, int color)
	{
		int scale = KorppuMod.mc.getWindow().getGuiScale();
		int x = (int)(x1 * scale);
		int y = (int)(y1 * scale);
		int w = (int)((x2 - x1) * scale);
		int h = (int)((y2 - y1) * scale);

		context.pose().pushMatrix();
		context.pose().scale(1F / scale);
		context.horizontalLine(x, x + w - 1, y, color);
		context.horizontalLine(x, x + w - 1, y + h - 1, color);
		context.verticalLine(x, y + 1, y + h - 1, color);
		context.verticalLine(x + w - 1, y + 1, y + h - 1, color);
		context.pose().popMatrix();
	}

	public static void drawLineStrip2D(GuiGraphicsExtractor context, float[][] vertices,
		int color)
	{
		if(vertices.length < 2)
			return;

		for(int i = 1; i < vertices.length; i++)
			drawLine2D(context, vertices[i - 1][0], vertices[i - 1][1],
				vertices[i][0], vertices[i][1], color);
		drawLine2D(context, vertices[vertices.length - 1][0],
			vertices[vertices.length - 1][1], vertices[0][0], vertices[0][1],
			color);
	}

	public static void drawBoxShadow2D(GuiGraphicsExtractor context, int x1, int y1,
		int x2, int y2)
	{
		int outlineColor = 0x5544FF80;
		drawBorder2D(context, x1, y1, x2, y2, outlineColor);

		float xs1 = x1 - 1;
		float xs2 = x2 + 1;
		float ys1 = y1 - 1;
		float ys2 = y2 + 1;

		int shadowColor1 = 0x5544FFC0;
		int shadowColor2 = 0x00000000;

		Matrix3x2f pose = new Matrix3x2f(context.pose());
		ScreenRectangle scissor = context.scissorStack.peek();

		context.guiRenderState.addGuiElement(new CustomQuadRenderState(pose, x1, y1,
			x2, y1, xs2, ys1, xs1, ys1, shadowColor1, shadowColor1,
			shadowColor2, shadowColor2, scissor));

		context.guiRenderState.addGuiElement(new CustomQuadRenderState(pose, xs1, ys1,
			xs1, ys2, x1, y2, x1, y1, shadowColor2, shadowColor2, shadowColor1,
			shadowColor1, scissor));

		context.guiRenderState.addGuiElement(new CustomQuadRenderState(pose, x2, y1,
			x2, y2, xs2, ys2, xs2, ys1, shadowColor1, shadowColor1,
			shadowColor2, shadowColor2, scissor));

		context.guiRenderState.addGuiElement(new CustomQuadRenderState(pose, x2, y2,
			x1, y2, xs1, ys2, xs2, ys2, shadowColor1, shadowColor1,
			shadowColor2, shadowColor2, scissor));
	}

	public record ColoredPoint(Vec3 point, int color)
	{}

	public record ColoredBox(AABB box, int color)
	{}
}
