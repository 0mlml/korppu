package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;

@Getter
@AllArgsConstructor
public class BreakBlockEvent extends Event {
    public BlockPos pos;
}
