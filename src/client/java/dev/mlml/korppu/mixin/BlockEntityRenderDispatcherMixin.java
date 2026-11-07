package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.BlockRenderEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(at = @At("HEAD"), method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", cancellable = true)
    private <S extends BlockEntityRenderState> void onRenderRenderState(S renderState, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        net.minecraft.world.level.block.state.BlockState blockState = KorppuMod.mc.level != null ? KorppuMod.mc.level.getBlockState(renderState.blockPos) : null;
        BlockRenderEvent event = new BlockRenderEvent(matrices, renderState.blockPos, blockState);
        KorppuMod.eventManager.trigger(event);

        if (event.isCancelled()) ci.cancel();
    }
}
