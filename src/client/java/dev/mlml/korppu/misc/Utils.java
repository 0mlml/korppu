package dev.mlml.korppu.misc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class Utils {
    public static boolean hasLOS(Vec3 start, Vec3 end, Level world, Entity requester) {
        ClipContext r = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, requester);
        BlockHitResult hit = world.clip(r);
        return hit.getLocation().equals(end);
    }

    public static Iterable<ChunkAccess> chunks(boolean onlyWithLoadedNeighbours) {
        return () -> new ChIterator(onlyWithLoadedNeighbours);
    }

    public static Iterable<ChunkAccess> chunks() {
        return chunks(false);
    }

    public static class TickManager {

        static final List<TickEntry> entries = new ArrayList<>();
        static final List<Runnable> nextTickRunners = new ArrayList<>();

        public static void runInNTicks(int n, Runnable toRun) {
            entries.add(new TickEntry(n, toRun));
        }

        public static void tick() {
            for (TickEntry entry : entries.toArray(new TickEntry[0])) {
                entry.v--;
                if (entry.v <= 0) {
                    entry.r.run();
                    entries.remove(entry);
                }
            }
        }

        public static void runOnNextRender(Runnable r) {
            nextTickRunners.add(r);
        }

        public static void render() {
            for (Runnable nextTickRunner : nextTickRunners) {
                nextTickRunner.run();
            }
            nextTickRunners.clear();
        }

        static class TickEntry {

            final Runnable r;
            int v;

            public TickEntry(int v, Runnable r) {
                this.v = v;
                this.r = r;
            }
        }
    }

    public static BitSet searchMatches(String original, String search) {
        int searchIndex = 0;
        BitSet matches = new BitSet();
        char[] chars = search.toLowerCase().toCharArray();
        String lower = original.toLowerCase();
        for (char aChar : chars) {
            if (searchIndex >= original.length()) {
                matches.clear();
                return matches;
            }
            int index;
            if ((index = lower.substring(searchIndex).indexOf(aChar)) >= 0) {
                matches.set(searchIndex + index);
                searchIndex += index + 1;
            } else {
                matches.clear();
                return matches;
            }
        }
        return matches;
    }

}
