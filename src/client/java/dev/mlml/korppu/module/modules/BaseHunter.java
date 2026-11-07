package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.ChunkDataEvent;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.LevelChunk;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BaseHunter extends Module {
    private final MultiSelectSetting modes = config.add(new MultiSelectSetting(
            "Modes",
            "Detections to run on incoming chunk data",
            () -> List.of(Mode.NEW_CHUNKS.name(), Mode.CAVE_DISTURBANCE.name(), Mode.SPAWNER.name(), Mode.PORTAL_PATTERN.name(), Mode.DECORATED_POT.name()),
            true,
            false,
            Set.of(Mode.NEW_CHUNKS.name())
    ));
    private final DoubleSetting maxChunks = config.add(new DoubleSetting("Max Chunks", "Maximum retained detections", 256d, 32d, 4096d, 0));
    private final BooleanSetting render = config.add(new BooleanSetting("Render", "Render detected chunk markers", true));
    private final DoubleSetting newChunkFlowingThreshold = config.add(
            new DoubleSetting("NewChunk Fluid Threshold", "Flowing fluid hits required for NEW_CHUNKS", 8d, 1d, 64d, 0)
                    .showOnlyIf(() -> modeEnabled(Mode.NEW_CHUNKS)));
    private final DoubleSetting newChunkSampleStep = config.add(
            new DoubleSetting("NewChunk Sample Step", "Sampling stride for NEW_CHUNKS scan", 2d, 1d, 8d, 0)
                    .showOnlyIf(() -> modeEnabled(Mode.NEW_CHUNKS)));
    private final DoubleSetting caveMaxY = config.add(
            new DoubleSetting("Disturbance Max Y", "Upper Y bound for cave disturbance scan", 50d, -64d, 128d, 0)
                    .showOnlyIf(() -> modeEnabled(Mode.CAVE_DISTURBANCE)));
    private final DoubleSetting caveSuspiciousThreshold = config.add(
            new DoubleSetting("Disturbance Threshold", "Suspicious block hits required", 6d, 1d, 64d, 0)
                    .showOnlyIf(() -> modeEnabled(Mode.CAVE_DISTURBANCE)));

    private final Map<Long, Detection> detections = new ConcurrentHashMap<>();
    private final Map<Long, List<PotDetection>> suspiciousPotsByChunk = new ConcurrentHashMap<>();

    private static final Set<Item> NATURAL_POT_ITEMS = Set.of(
            Items.AIR,
            Items.STRING,
            Items.EMERALD,
            Items.EMERALD_BLOCK,
            Items.RAW_IRON_BLOCK,
            Items.IRON_INGOT,
            Items.TRIAL_KEY,
            Items.DIAMOND,
            Items.DIAMOND_BLOCK,
            Items.MUSIC_DISC_CREATOR_MUSIC_BOX
    );

    public BaseHunter() {
        super("BaseHunter", "Scans chunk data for base-related anomalies", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        detections.clear();
        suspiciousPotsByChunk.clear();
    }

    @Listener
    public void onChunkData(ChunkDataEvent event) {
        if (!super.isEnabled() || event.getChunk() == null) return;

        LevelChunk chunk = event.getChunk();
        EnumSet<Mode> found = EnumSet.noneOf(Mode.class);

        if (modeEnabled(Mode.NEW_CHUNKS) && looksLikeNewChunk(chunk, newChunkFlowingThreshold.getValue().intValue(), newChunkSampleStep.getValue().intValue())) {
            found.add(Mode.NEW_CHUNKS);
        }
        if (modeEnabled(Mode.CAVE_DISTURBANCE) && hasCaveDisturbance(chunk, caveMaxY.getValue().intValue(), caveSuspiciousThreshold.getValue().intValue())) {
            found.add(Mode.CAVE_DISTURBANCE);
        }
        if (modeEnabled(Mode.SPAWNER) && containsBlock(chunk, Blocks.SPAWNER)) {
            found.add(Mode.SPAWNER);
        }
        if (modeEnabled(Mode.PORTAL_PATTERN)
                && (containsBlock(chunk, Blocks.NETHER_PORTAL) || containsBlock(chunk, Blocks.END_PORTAL_FRAME))) {
            found.add(Mode.PORTAL_PATTERN);
        }
        if (modeEnabled(Mode.DECORATED_POT)) {
            List<PotDetection> pots = findSuspiciousPots(chunk);
            if (!pots.isEmpty()) {
                found.add(Mode.DECORATED_POT);
                suspiciousPotsByChunk.put(chunk.getPos().pack(), pots);
            }
        }

        if (!found.isEmpty()) {
            long key = chunk.getPos().pack();
            detections.compute(key, (k, old) -> {
                if (old == null) {
                    return new Detection(chunk.getPos(), found);
                }
                old.modes.addAll(found);
                old.lastSeenTick = KorppuMod.mc.level != null ? KorppuMod.mc.level.getOverworldClockTime() : old.lastSeenTick;
                return old;
            });
            trimDetections();
        }
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickProgress) {
        if (!render.getValue() || KorppuMod.mc.level == null) return;
        for (Detection detection : detections.values()) {
            int color = 0xFF00CCFF;
            if (detection.modes.contains(Mode.SPAWNER)) color = 0xFFFFAA00;
            if (detection.modes.contains(Mode.PORTAL_PATTERN)) color = 0xFFFF55FF;
            if (detection.modes.contains(Mode.CAVE_DISTURBANCE)) color = 0xFFFF7744;
            if (detection.modes.contains(Mode.NEW_CHUNKS) && detection.modes.size() == 1) color = 0xFF55FF55;
            if (detection.modes.contains(Mode.DECORATED_POT)) color = 0xFFFF55AA;

            int x1 = detection.pos.getMinBlockX();
            int z1 = detection.pos.getMinBlockZ();
            int x2 = detection.pos.getMaxBlockX() + 1;
            int z2 = detection.pos.getMaxBlockZ() + 1;
            AABB box = new AABB(x1, KorppuMod.mc.level.getMinY(), z1, x2, KorppuMod.mc.level.getMaxY() + 1, z2);

            RenderUtils.drawOutlinedBox(matrices, box, color, false);
        }

        if (modeEnabled(Mode.DECORATED_POT)) {
            for (List<PotDetection> list : suspiciousPotsByChunk.values()) {
                for (PotDetection pot : list) {
                    RenderUtils.drawOutlinedBox(matrices, new AABB(pot.pos()), 0xFFFF55AA, false);
                }
            }
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        long newChunks = detections.values().stream().filter(d -> d.modes.contains(Mode.NEW_CHUNKS)).count();
        long disturbed = detections.values().stream().filter(d -> d.modes.contains(Mode.CAVE_DISTURBANCE)).count();
        long spawners = detections.values().stream().filter(d -> d.modes.contains(Mode.SPAWNER)).count();
        long portals = detections.values().stream().filter(d -> d.modes.contains(Mode.PORTAL_PATTERN)).count();
        long pots = suspiciousPotsByChunk.values().stream().mapToLong(List::size).sum();

        return List.of(
                StatusEntry.text("Detections", String.valueOf(detections.size())),
                StatusEntry.text("NewChunks", String.valueOf(newChunks)),
                StatusEntry.text("Disturbance", String.valueOf(disturbed)),
                StatusEntry.text("Spawners", String.valueOf(spawners)),
                StatusEntry.text("Portals", String.valueOf(portals)),
                StatusEntry.text("Pots", String.valueOf(pots))
        );
    }

    public PotDetection getNearestSuspiciousPot(Vec3 origin, double maxDistance) {
        if (origin == null) return null;

        double bestSq = maxDistance * maxDistance;
        PotDetection best = null;
        for (List<PotDetection> list : suspiciousPotsByChunk.values()) {
            for (PotDetection pot : list) {
                double sq = pot.pos().getCenter().distanceToSqr(origin);
                if (sq < bestSq) {
                    bestSq = sq;
                    best = pot;
                }
            }
        }
        return best;
    }

    private boolean modeEnabled(Mode mode) {
        return modes.isSelected(mode.name());
    }

    private static boolean looksLikeNewChunk(LevelChunk chunk, int threshold, int sampleStep) {
        int step = Math.max(1, sampleStep);
        int hitThreshold = Math.max(1, threshold);
        int flowingFluidCount = 0;

        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        if (maxY < minY) maxY = minY + 64;

        for (int x = 0; x < 16; x += step) {
            for (int z = 0; z < 16; z += step) {
                for (int y = minY; y <= maxY; y += step) {
                    var state = chunk.getBlockState(chunk.getPos().getBlockAt(x, y, z));
                    var fluid = state.getFluidState();
                    if (!fluid.isEmpty() && !fluid.isSource()) {
                        flowingFluidCount++;
                        if (flowingFluidCount >= hitThreshold) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean containsBlock(LevelChunk chunk, net.minecraft.world.level.block.Block target) {
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        if (maxY < minY) maxY = minY + 64;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (chunk.getBlockState(chunk.getPos().getBlockAt(x, y, z)).getBlock() == target) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasCaveDisturbance(LevelChunk chunk, int maxScanY, int suspiciousThreshold) {
        int threshold = Math.max(1, suspiciousThreshold);
        int minY = chunk.getMinY();
        int maxY = Math.min(chunk.getMaxY(), maxScanY);
        int suspicious = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    var block = chunk.getBlockState(chunk.getPos().getBlockAt(x, y, z)).getBlock();
                    if (block == Blocks.TORCH
                            || block == Blocks.WALL_TORCH
                            || block == Blocks.RAIL
                            || block == Blocks.CHEST
                            || block == Blocks.COBBLESTONE
                            || block == Blocks.COBBLESTONE_STAIRS
                            || block == Blocks.CRAFTING_TABLE
                            || block == Blocks.FURNACE
                            || block == Blocks.BARREL) {
                        suspicious++;
                        if (suspicious >= threshold) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static List<PotDetection> findSuspiciousPots(LevelChunk chunk) {
        Set<BlockPos> seen = new HashSet<>();
        List<PotDetection> found = new java.util.ArrayList<>();

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof DecoratedPotBlockEntity pot)) continue;
            Item item = pot.getTheItem().getItem();
            if (NATURAL_POT_ITEMS.contains(item)) continue;
            if (seen.add(pot.getBlockPos())) {
                found.add(new PotDetection(pot.getBlockPos().immutable(), item));
            }
        }

        return found;
    }

    private void trimDetections() {
        int cap = maxChunks.getValue().intValue();
        if (detections.size() <= cap) return;

        List<Map.Entry<Long, Detection>> sorted = new java.util.ArrayList<>(detections.entrySet());
        sorted.sort((a, b) -> Long.compare(a.getValue().lastSeenTick, b.getValue().lastSeenTick));
        int removeCount = detections.size() - cap;
        for (int i = 0; i < removeCount && i < sorted.size(); i++) {
            Long key = sorted.get(i).getKey();
            detections.remove(key);
            suspiciousPotsByChunk.remove(key);
        }
    }

    public record PotDetection(BlockPos pos, Item item) {
        public String itemName() {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
    }

    private static class Detection {
        private final ChunkPos pos;
        private final EnumSet<Mode> modes;
        private long lastSeenTick;

        private Detection(ChunkPos pos, EnumSet<Mode> modes) {
            this.pos = pos;
            this.modes = EnumSet.copyOf(modes);
            this.lastSeenTick = KorppuMod.mc.level != null ? KorppuMod.mc.level.getOverworldClockTime() : 0;
        }
    }

    public enum Mode {
        NEW_CHUNKS,
        CAVE_DISTURBANCE,
        SPAWNER,
        PORTAL_PATTERN,
        DECORATED_POT
    }
}
