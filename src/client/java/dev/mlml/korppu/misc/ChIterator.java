package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.mixin.ClientChunkManagerAccessor;
import dev.mlml.korppu.mixin.ClientChunkMapAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Iterator;

public class ChIterator implements Iterator<ChunkAccess> {
    private final ClientChunkMapAccessor map = (ClientChunkMapAccessor) (Object) ((ClientChunkManagerAccessor) KorppuMod.mc.level.getChunkSource()).getStorage();
    private final boolean onlyWithLoadedNeighbours;

    private int i = 0;
    private ChunkAccess chunk;

    public ChIterator(boolean onlyWithLoadedNeighbours) {
        this.onlyWithLoadedNeighbours = onlyWithLoadedNeighbours;

        getNext();
    }

    private ChunkAccess getNext() {
        ChunkAccess prev = chunk;
        chunk = null;

        while (i < map.getChunks().length()) {
            chunk = map.getChunks().get(i++);
            if (chunk != null && (!onlyWithLoadedNeighbours || isInRadius(chunk))) break;
        }

        return prev;
    }

    private boolean isInRadius(ChunkAccess chunk) {
        int x = chunk.getPos().x();
        int z = chunk.getPos().z();

        return KorppuMod.mc.level.hasChunk(x + 1, z) && KorppuMod.mc.level.hasChunk(x - 1, z) && KorppuMod.mc.level.hasChunk(x, z + 1) && KorppuMod.mc.level.hasChunk(x, z - 1);
    }

    @Override
    public boolean hasNext() {
        return chunk != null;
    }

    @Override
    public ChunkAccess next() {
        return getNext();
    }
}