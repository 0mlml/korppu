package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.ChunkDataEvent;
import dev.mlml.korppu.event.events.SeedChangedEvent;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.misc.SeedState;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class OreSim extends Module {
    private final ListSetting<OreType> oreType = config.add(new ListSetting<>("Ore", "Ore type to simulate", OreType.DIAMOND));
    private final DoubleSetting attempts = config.add(new DoubleSetting("Attempts", "Approximate vein attempts per chunk", 24d, 1d, 256d, 0));
    private final BooleanSetting renderVeins = config.add(new BooleanSetting("Render Veins", "Render small vein clouds instead of single points", true));
    private final DoubleSetting maxChunks = config.add(new DoubleSetting("Max Chunks", "Maximum cached chunks", 256d, 16d, 4096d, 0));

    private final Map<Long, List<BlockPos>> cache = new ConcurrentHashMap<>();

    public OreSim() {
        super("OreSim", "Approximate ore simulation from world seed", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        cache.clear();
    }

    @Listener
    public void onSeedChanged(SeedChangedEvent event) {
        cache.clear();
    }

    @Listener
    public void onChunkData(ChunkDataEvent event) {
        if (!isEnabled() || event.getChunk() == null) return;
        Long seed = SeedState.getSeed();
        if (seed == null) return;

        if (KorppuMod.mc.level == null || !oreType.getValue().dimensionAllowed(KorppuMod.mc.level)) {
            return;
        }

        LevelChunk chunk = event.getChunk();
        long key = chunk.getPos().pack();
        cache.put(key, simulateChunk(seed, chunk));

        int cap = maxChunks.getValue().intValue();
        if (cache.size() > cap) {
            Long first = cache.keySet().stream().findFirst().orElse(null);
            if (first != null) cache.remove(first);
        }
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickProgress) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;

        for (List<BlockPos> list : cache.values()) {
            for (BlockPos pos : list) {
                if (KorppuMod.mc.player.blockPosition().distSqr(pos) > 256 * 256) continue;
                AABB box = new AABB(pos);
                RenderUtils.drawOutlinedBox(matrices, box, oreType.getValue().color, false);
            }
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        int total = cache.values().stream().mapToInt(List::size).sum();
        return List.of(
                StatusEntry.text("Ore", oreType.getValue().name()),
                StatusEntry.text("Seed", SeedState.getSeed() == null ? "unset" : "set"),
                StatusEntry.text("Predicted", String.valueOf(total))
        );
    }

    private List<BlockPos> simulateChunk(long seed, LevelChunk chunk) {
        ArrayList<BlockPos> out = new ArrayList<>();
        OreType type = oreType.getValue();
        int minY = KorppuMod.mc.level.getMinY();
        int maxY = KorppuMod.mc.level.getMaxY();

        long mixed = seed
                ^ ((long) chunk.getPos().x() * 341873128712L)
                ^ ((long) chunk.getPos().z() * 132897987541L)
                ^ type.ordinal() * 9973L;
        Random random = new Random(mixed);

        int tries = attempts.getValue().intValue();
        for (int i = 0; i < tries; i++) {
            int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
            int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
            int y = type.pickY(random, minY, maxY);

            BlockPos center = new BlockPos(x, y, z);
            if (!renderVeins.getValue()) {
                out.add(center);
                continue;
            }

            int size = type.pickVeinSize(random);
            for (int j = 0; j < size; j++) {
                int ox = random.nextInt(5) - 2;
                int oy = random.nextInt(3) - 1;
                int oz = random.nextInt(5) - 2;
                int px = center.getX() + ox;
                int py = Math.max(minY, Math.min(maxY, center.getY() + oy));
                int pz = center.getZ() + oz;

                if (px < chunk.getPos().getMinBlockX() || px > chunk.getPos().getMaxBlockX()) continue;
                if (pz < chunk.getPos().getMinBlockZ() || pz > chunk.getPos().getMaxBlockZ()) continue;

                out.add(new BlockPos(px, py, pz));
            }
        }

        return out;
    }

    public enum OreType {
        DIAMOND(0xFF55FFFF),
        ANCIENT_DEBRIS(0xFFFF9955),
        NETHER_GOLD(0xFFFFCC55),
        EMERALD(0xFF55FF55),
        GOLD(0xFFFFFF55);

        private final int color;

        OreType(int color) {
            this.color = color;
        }

        private int pickY(Random random, int minY, int maxY) {
            return switch (this) {
                case DIAMOND -> random.nextInt(Math.max(1, Math.min(20, maxY - minY + 1))) + minY;
                case ANCIENT_DEBRIS -> random.nextInt(Math.max(1, Math.min(32, maxY - minY + 1))) + Math.max(minY, 8);
                case NETHER_GOLD -> random.nextInt(Math.max(1, Math.min(96, maxY - minY + 1))) + minY;
                case EMERALD -> random.nextInt(Math.max(1, Math.min(192, maxY - minY + 1))) + minY;
                case GOLD -> random.nextInt(Math.max(1, Math.min(64, maxY - minY + 1))) + minY;
            };
        }

        private int pickVeinSize(Random random) {
            return switch (this) {
                case DIAMOND -> 2 + random.nextInt(6);
                case ANCIENT_DEBRIS -> 1 + random.nextInt(3);
                case NETHER_GOLD -> 4 + random.nextInt(8);
                case EMERALD -> 1 + random.nextInt(2);
                case GOLD -> 3 + random.nextInt(8);
            };
        }

        private boolean dimensionAllowed(Level world) {
            var dim = world.dimension();
            return switch (this) {
                case ANCIENT_DEBRIS, NETHER_GOLD -> dim == Level.NETHER;
                default -> dim == Level.OVERWORLD;
            };
        }
    }
}
