package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.chunk.LevelChunk;

@Getter
@Setter
@AllArgsConstructor
public class ChunkDataEvent extends Event {
    LevelChunk chunk;
}
