package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

@Getter
@Setter
@AllArgsConstructor
public class BlockUpdateEvent extends Event {
    public BlockPos pos;
    public BlockState oldState;
    public BlockState newState;

}
