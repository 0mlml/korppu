package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(at = @At("TAIL"), method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
    private void korppu$renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (KorppuMod.mc.getDebugOverlay().showDebugScreen()) return;

        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
        ModuleManager.renderHud(context, tickDelta);
    }
}
