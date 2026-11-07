package dev.mlml.korppu.mixin;

import com.mojang.blaze3d.TracyFrameCapture;
import dev.mlml.korppu.imgui.ImGuiManager;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.module.AirPlaceApi;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.AirPlace;
import dev.mlml.korppu.module.modules.MultiUse;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.opengl.GL30C;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    @Shadow
    protected abstract void startUseItem();

    @Shadow
    private int rightClickDelay;

    @Shadow
    public HitResult hitResult;

    @Unique
    private boolean korppu$runningMultiUse;


    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void korppu$interceptPauseMenu(boolean pauseOnly, CallbackInfo ci) {
        if (KorppuImGui.isMenuOpen()) {
            ci.cancel();
        }
    }

    @Redirect(method = "startUseItem",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
                    ordinal = 1))
    private HitResult airplace$redirectCrosshairTarget(Minecraft mc) {
        HitResult original = mc.hitResult;
        AirPlace ap = ModuleManager.getModule(AirPlace.class);
        if (ap == null || !ap.isEnabled()) return original;
        if (original == null || original.getType() != HitResult.Type.MISS) return original;

        BlockHitResult synthetic = AirPlaceApi.findPlacementTarget(mc, ap.getEffectiveRange());
        return synthetic != null ? synthetic : original;
    }

    @Inject(method = "handleKeybinds",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;startUseItem()V",
                    shift = At.Shift.AFTER))
    private void korppu$multiUse(CallbackInfo ci) {
        if (korppu$runningMultiUse) return;

        MultiUse multiUse = ModuleManager.getModule(MultiUse.class);
        if (multiUse == null || !multiUse.isEnabled()) return;

        if (multiUse.isOnlyOnMiss() && (hitResult == null || hitResult.getType() != HitResult.Type.MISS)) {
            return;
        }

        korppu$runningMultiUse = true;
        try {
            int extra = Math.max(0, multiUse.getExtraUses());
            for (int i = 0; i < extra; i++) {
                startUseItem();
            }
        } finally {
            korppu$runningMultiUse = false;
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I"))
    public int replaceItemUseCooldown(Minecraft minecraftClient) {
        Passives pm = ModuleManager.getModule(Passives.class);
        if (pm == null) {
            return this.rightClickDelay;
        }
        if (pm.isEnabled() && pm.getNoPlaceDelay().getValue()) {
            return 0;
        } else {
            return this.rightClickDelay;
        }
    }

    // Render ImGui just before the buffer swap so it lands on top of the final frame.
    @Inject(method = "renderFrame",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
                    shift = At.Shift.BEFORE))
    private void korppu$renderImGui(boolean bl, CallbackInfo ci) {
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        ImGuiManager.startFrame();
        KorppuImGui.render();
        ImGuiManager.endFrame();
    }
}
