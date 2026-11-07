package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.BlockRenderEvent;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public class BlockRenderManagerMixin {
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true, require = 0)
    void dispatchRenderEvent(BlockState state, BlockPos pos, BlockGetter world, PoseStack matrices, VertexConsumer vertexConsumer, boolean cull, List<BlockStateModelPart> parts, CallbackInfo ci) {
        BlockRenderEvent re = new BlockRenderEvent(matrices, pos, state);
        KorppuMod.eventManager.trigger(re);
        if (re.isCancelled()) {
            ci.cancel();
        }
    }
}