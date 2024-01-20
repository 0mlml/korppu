package dev.mlml.korppu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.mlml.korppu.misc.Renderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
    @Shadow
    @Final
    MinecraftClient client;
    @Shadow
    @Final
    private Camera camera;

    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Shadow
    public abstract void loadProjectionMatrix(Matrix4f projectionMatrix);

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double fov);

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    void noViewBob(float tickDelta)
    {
        MatrixStack ms = Renderer.WRLDR.getEmptyMatrixStack();
        double d = this.getFov(camera, tickDelta, true);
        ms.peek().getPositionMatrix().mul(this.getBasicProjectionMatrix(d));
        loadProjectionMatrix(ms.peek().getPositionMatrix());
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
    void onWorldRender_(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci)
    {
        RenderSystem.backupProjectionMatrix();
        noViewBob(tickDelta);
        MatrixStack ms = Renderer.WRLDR.getEmptyMatrixStack();
        ms.push();
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        Renderer.WRLDR.renderActions();
        ms.pop();
        RenderSystem.restoreProjectionMatrix();
    }
}
