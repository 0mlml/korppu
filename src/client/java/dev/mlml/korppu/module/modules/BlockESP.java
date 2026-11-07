package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.BlockUpdateEvent;
import dev.mlml.korppu.event.events.ChunkDataEvent;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.misc.Utils;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.module.ModuleManager;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BlockESP extends Module {
    private final Map<Long, ESPChunk> chunks = new ConcurrentHashMap<>();
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor();
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    @Getter
    private final MultiSelectSetting blockList = config.add(new MultiSelectSetting(
            "Block List", "Blocks to highlight",
            () -> BuiltInRegistries.BLOCK.stream()
                    .map(b -> BuiltInRegistries.BLOCK.getKey(b).toString())
                    .filter(id -> !id.equals("minecraft:air"))
                    .sorted()
                    .collect(Collectors.toList()),
            true, false,
            new LinkedHashSet<>(Arrays.asList(
                    "minecraft:chest", "minecraft:ender_chest", "minecraft:barrel",
                    "minecraft:shulker_box", "minecraft:hopper", "minecraft:dispenser",
                    "minecraft:brewing_stand", "minecraft:furnace",
                    "minecraft:blast_furnace", "minecraft:smoker"
            ))));
    @Getter
    private final DoubleSetting maxBlocks = config.add(new DoubleSetting("Chunk max blocks", "Maximum blocks to show", 16d, 1d, 4096d, 0));
    @Getter
    private final DoubleSetting chunkRange = config.add(new DoubleSetting("Chunk Range", "How many chunks from player to keep/render", 9d, 1d, 32d, 0));
    private final BooleanSetting logToChat = config.add(new BooleanSetting("LogToChat", "Log the blocks to chat", false));
    private final BooleanSetting throughWalls = config.add(new BooleanSetting("Through Walls", "Render highlights through walls", true));
    private final BooleanSetting outline = config.add(new BooleanSetting("Outline", "Draw an outline around highlighted blocks", true));

    public BlockESP() {
        super("BlockESP", "Shows all the wanted blocks in the area", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        synchronized (chunks) {
            chunks.clear();
        }

        if (KorppuMod.mc.level == null) return;

        for (ChunkAccess chunk : Utils.chunks()) {
            searchChunk(chunk);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        synchronized (chunks) {
            chunks.clear();
        }
    }

    @Listener
    public void onChunkData(ChunkDataEvent event) {
        searchChunk(event.getChunk());
    }

    private void searchChunk(ChunkAccess chunk) {
        workerThread.submit(() -> {
            if (!this.isEnabled() || KorppuMod.mc.level == null) return;

            ESPChunk espChunk = ESPChunk.searchChunk(chunk, getTargetBlocks());

            if (espChunk.size() > 0) {
                synchronized (chunks) {
                    chunks.put(chunk.getPos().pack(), espChunk);
                    espChunk.update();

                    updateChunk(chunk.getPos().x() - 1, chunk.getPos().z());
                    updateChunk(chunk.getPos().x() + 1, chunk.getPos().z());
                    updateChunk(chunk.getPos().x(), chunk.getPos().z() - 1);
                    updateChunk(chunk.getPos().x(), chunk.getPos().z() + 1);
                }
            }
        });
    }

    @Listener
    public void onBlockUpdate(BlockUpdateEvent event) {
        if (!this.isEnabled()) return;

        int bx = event.pos.getX();
        int by = event.pos.getY();
        int bz = event.pos.getZ();

        int chunkX = bx >> 4;
        int chunkZ = bz >> 4;
        long key = ChunkPos.pack(chunkX, chunkZ);

        boolean added = isTargeted(event.newState.getBlock()) && !isTargeted(event.oldState.getBlock());
        boolean removed = !added && !isTargeted(event.newState.getBlock()) && isTargeted(event.oldState.getBlock());

        if (added || removed) {
            workerThread.submit(() -> {
                synchronized (chunks) {
                    ESPChunk chunk = chunks.get(key);

                    if (chunk == null) {
                        chunk = new ESPChunk(chunkX, chunkZ);
                        if (chunk.shouldBeDeleted()) return;
                        chunks.put(key, chunk);
                    }

                    blockPos.set(bx, by, bz);

                    if (added) {
                        chunk.add(blockPos);
                        if (logToChat.getValue()) {
                            ChatHelper.message(String.format("Found %s at %s",
                                    event.newState.getBlock().getName(), blockPos.toShortString()));
                        }
                    } else {
                        chunk.remove(blockPos);
                    }

                    for (int x = -1; x < 2; x++) {
                        for (int z = -1; z < 2; z++) {
                            for (int y = -1; y < 2; y++) {
                                if (x == 0 && y == 0 && z == 0) continue;
                                updateBlock(bx + x, by + y, bz + z);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void afterTick() {
        if (KorppuMod.mc.level != null && KorppuMod.mc.player != null) {
            synchronized (chunks) {
                chunks.entrySet().removeIf(entry -> entry.getValue().shouldBeDeleted());
            }
        }
    }

    private void updateChunk(int x, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.pack(x, z));
        if (chunk != null) chunk.update();
    }

    private void updateBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
        if (chunk != null) chunk.update(x, y, z);
    }

    public ESPBlock getBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
        return chunk == null ? null : chunk.get(x, y, z);
    }

    private List<Block> getTargetBlocks() {
        return blockList.getValue().stream()
                .map(id -> (Block) BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null))
                .filter(b -> b != null && b != Blocks.AIR)
                .collect(Collectors.toList());
    }

    private boolean isTargeted(Block block) {
        String registryName = BuiltInRegistries.BLOCK.getKey(block).toString();
        return blockList.isSelected(registryName);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        int totalBlocks = 0;
        int chunkCount;
        synchronized (chunks) {
            chunkCount = chunks.size();
            for (ESPChunk chunk : chunks.values()) totalBlocks += chunk.size();
        }
        int max = maxBlocks.getValue().intValue() * Math.max(1, chunkCount);
        return List.of(
                StatusEntry.bar("Blocks", totalBlocks + " found", (float) totalBlocks / Math.max(1, max)),
                StatusEntry.text("Chunks", chunkCount + " tracked"),
                StatusEntry.text("Range", chunkRange.getValue().intValue() + " chunks"));
    }

    @Override
    public String getStatus() {
        int totalBlocks = 0;
        synchronized (chunks) {
            for (ESPChunk chunk : chunks.values()) {
                totalBlocks += chunk.size();
            }
        }
        return totalBlocks + " blocks";
    }

    @Override
    public void onWorldRender(PoseStack matrixStack, float tickProgress) {
        synchronized (chunks) {
            for (ESPChunk chunk : chunks.values()) {
                if (chunk.shouldBeDeleted()) continue;

                if (chunk.blocks != null) {
                    for (ESPBlock block : chunk.blocks.values()) {
                        if (!block.loaded) continue;

                        AABB pos = new AABB(block.x, block.y, block.z,
                                block.x + 1, block.y + 1, block.z + 1);
                        int color = 0x5086FF00;

                        RenderUtils.drawSolidBox(
                                matrixStack,
                                pos,
                                color,
                                !throughWalls.getValue()
                        );

                        if (outline.getValue()) {
                            RenderUtils.drawOutlinedBox(
                                    matrixStack,
                                    pos,
                                    0xFF86FF00,
                                    !throughWalls.getValue()
                            );
                        }
                    }
                }
            }
        }
    }

    public static class ESPChunk {
        private final int x, z;
        public Map<Long, ESPBlock> blocks;

        public ESPChunk(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public ESPBlock get(int x, int y, int z) {
            return blocks == null ? null : blocks.get(ESPBlock.getKey(x, y, z));
        }

        public void add(BlockPos blockPos, boolean update) {
            ESPBlock block = new ESPBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            if (blocks == null) blocks = new ConcurrentHashMap<>(64);
            blocks.put(ESPBlock.getKey(blockPos), block);

            if (update) block.update();
        }

        public void add(BlockPos blockPos) {
            add(blockPos, true);
        }

        public void remove(BlockPos blockPos) {
            if (blocks != null) {
                ESPBlock block = blocks.remove(ESPBlock.getKey(blockPos));
                if (block != null) block.loaded = false;
            }
        }

        public void update() {
            if (blocks != null) {
                for (ESPBlock block : blocks.values()) block.update();
            }
        }

        public void update(int x, int y, int z) {
            if (blocks != null) {
                ESPBlock block = blocks.get(ESPBlock.getKey(x, y, z));
                if (block != null) block.update();
            }
        }

        public int size() {
            return blocks == null ? 0 : blocks.size();
        }

        public boolean shouldBeDeleted() {
            if (KorppuMod.mc.player == null) return true;

            int range = ModuleManager.getModule(BlockESP.class).chunkRange.getValue().intValue();
            int chunkX = KorppuMod.mc.player.blockPosition().getX() >> 4;
            int chunkZ = KorppuMod.mc.player.blockPosition().getZ() >> 4;

            return x > chunkX + range || x < chunkX - range || z > chunkZ + range || z < chunkZ - range;
        }

        public static ESPChunk searchChunk(ChunkAccess chunk, List<Block> blocks) {
            ESPChunk schunk = new ESPChunk(chunk.getPos().x(), chunk.getPos().z());
            if (schunk.shouldBeDeleted()) return schunk;

            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

            for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                if (schunk.size() > ModuleManager.getModule(BlockESP.class).maxBlocks.getValue()) break;
                for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                    if (schunk.size() > ModuleManager.getModule(BlockESP.class).maxBlocks.getValue()) break;
                    int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                    for (int y = KorppuMod.mc.level.getMinY(); y < height; y++) {
                        if (schunk.size() > ModuleManager.getModule(BlockESP.class).maxBlocks.getValue()) break;
                        blockPos.set(x, y, z);
                        BlockState bs = chunk.getBlockState(blockPos);

                        if (blocks.contains(bs.getBlock())) {
                            schunk.add(blockPos, false);
                        }
                    }
                }
            }

            return schunk;
        }
    }

    public static class ESPBlock {
        private static final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        public final int x, y, z;
        private BlockState state;
        public boolean loaded = true;

        public ESPBlock(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void update() {
            if (KorppuMod.mc.level == null) return;
            state = KorppuMod.mc.level.getBlockState(blockPos.set(x, y, z));
        }

        public static long getKey(int x, int y, int z) {
            return ((long) y << 16) | ((long) (z & 15) << 8) | ((long) (x & 15));
        }

        public static long getKey(BlockPos blockPos) {
            return getKey(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }
}
