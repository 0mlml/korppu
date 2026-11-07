package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;

@Getter
@Setter
@AllArgsConstructor
public class BlockRenderEvent extends Event {
    PoseStack matrixStack;
    BlockPos pos;
    BlockState state;
}