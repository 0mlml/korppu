package dev.mlml.korppu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private static String korppu$activeShader = "none";

    @Inject(method = "render", at = @At("TAIL"))
    private void korppu$render(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        applyPassiveShader();
    }

    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    args = "ldc=hand"))
    private void korppu$renderWorld(DeltaTracker tickCounter, CallbackInfo ci,
            @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix,
            @Local(name = "bobStack") PoseStack bobStack) {
        RenderSystem.getModelViewStack().pushMatrix().mul(modelViewMatrix);
        ModuleManager.renderWorld(bobStack, tickCounter.getGameTimeDeltaPartialTick(false));
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(at = @At("HEAD"), method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F", cancellable = true)
    private static void getNightVisionStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        Passives pm = ModuleManager.getModule(Passives.class);
        if (pm != null && pm.getNightVision().getValue()) cir.setReturnValue(5f);
    }

    @Unique
    private void applyPassiveShader() {
        Passives passives = ModuleManager.getModule(Passives.class);
        String wanted = "none";
        if (passives != null && passives.isEnabled()) {
            wanted = passives.getShaderMode().getValue().shaderId();
        }

        if (wanted.equals(korppu$activeShader)) return;

        try {
            if (wanted.equals("none")) {
                invokeNoArg(this, "disablePostProcessor", "disablePostEffect");
                korppu$activeShader = wanted;
                return;
            }

            Identifier id = Identifier.withDefaultNamespace(wanted);
            boolean loaded = invokeIdentifier(this, id, "loadPostProcessor", "loadPostEffect", "setPostProcessor");
            if (loaded) {
                korppu$activeShader = wanted;
            }
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static void invokeNoArg(Object target, String... names) {
        for (String name : names) {
            try {
                var method = target.getClass().getMethod(name);
                method.invoke(target);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    @Unique
    private static boolean invokeIdentifier(Object target, Identifier id, String... names) {
        for (String name : names) {
            try {
                var method = target.getClass().getMethod(name, Identifier.class);
                method.invoke(target, id);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
