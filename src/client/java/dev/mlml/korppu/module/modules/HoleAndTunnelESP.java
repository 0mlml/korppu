package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HoleAndTunnelESP extends Module {
    private static final Direction[] DIRECTIONS = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};

    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Scan range", 12d, 4d, 64d, 0));
    private final BooleanSetting holes = config.add(new BooleanSetting("Holes", "Render secure holes", true));
    private final BooleanSetting tunnels = config.add(new BooleanSetting("Tunnels", "Render 1x2 air tunnels", true));
    private final BooleanSetting staircases = config.add(new BooleanSetting("Staircases", "Render staircases", true));
    private final DoubleSetting minStaircaseLength = config.add(new DoubleSetting("Min Stair Len", "Minimum staircase step count", 3d, 1d, 20d, 0));
    private final DoubleSetting minStaircaseHeight = config.add(new DoubleSetting("Min Stair Height", "Minimum staircase tunnel height", 3d, 2d, 10d, 0));
    private final DoubleSetting maxStaircaseHeight = config.add(new DoubleSetting("Max Stair Height", "Maximum staircase tunnel height", 5d, 2d, 10d, 0));

    private final List<AABB> holeBoxes = new ArrayList<>();
    private final List<AABB> tunnelBoxes = new ArrayList<>();
    private final List<AABB> staircaseBoxes = new ArrayList<>();

    public HoleAndTunnelESP() {
        super("HoleAndTunnelESP", "Finds and renders nearby holes and tunnels", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;

        holeBoxes.clear();
        tunnelBoxes.clear();
        staircaseBoxes.clear();

        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > range.getValue() * range.getValue()) continue;

                    if (holes.getValue() && isHole(pos)) {
                        holeBoxes.add(new AABB(pos));
                    }

                    if (tunnels.getValue() && isTunnel(pos)) {
                        tunnelBoxes.add(new AABB(pos));
                    }

                    if (staircases.getValue()) {
                        checkStaircase(pos);
                    }
                }
            }
        }
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickProgress) {
        if (holes.getValue()) {
            for (AABB box : holeBoxes) {
                RenderUtils.drawOutlinedBox(matrices, box, 0xFF55FF55, false);
            }
        }

        if (tunnels.getValue()) {
            for (AABB box : tunnelBoxes) {
                RenderUtils.drawOutlinedBox(matrices, box, 0xFF55AAFF, false);
            }
        }

        if (staircases.getValue()) {
            for (AABB box : staircaseBoxes) {
                RenderUtils.drawOutlinedBox(matrices, box, 0xFFFF55FF, false);
            }
        }
    }

    private boolean isHole(BlockPos pos) {
        if (!KorppuMod.mc.level.getBlockState(pos).isAir()) return false;
        if (!KorppuMod.mc.level.getBlockState(pos.above()).isAir()) return false;
        if (!KorppuMod.mc.level.getBlockState(pos.above(2)).isAir()) return false;

        Block down = KorppuMod.mc.level.getBlockState(pos.below()).getBlock();
        if (!isBlastResistant(down)) return false;

        return isBlastResistant(KorppuMod.mc.level.getBlockState(pos.north()).getBlock())
                && isBlastResistant(KorppuMod.mc.level.getBlockState(pos.south()).getBlock())
                && isBlastResistant(KorppuMod.mc.level.getBlockState(pos.east()).getBlock())
                && isBlastResistant(KorppuMod.mc.level.getBlockState(pos.west()).getBlock());
    }

    private boolean isTunnel(BlockPos pos) {
        if (!KorppuMod.mc.level.getBlockState(pos).isAir()) return false;
        if (!KorppuMod.mc.level.getBlockState(pos.above()).isAir()) return false;
        if (!isBlastResistant(KorppuMod.mc.level.getBlockState(pos.below()).getBlock())) return false;

        int wallsX = (isBlastResistant(KorppuMod.mc.level.getBlockState(pos.north()).getBlock()) ? 1 : 0)
                + (isBlastResistant(KorppuMod.mc.level.getBlockState(pos.south()).getBlock()) ? 1 : 0);
        int wallsZ = (isBlastResistant(KorppuMod.mc.level.getBlockState(pos.east()).getBlock()) ? 1 : 0)
                + (isBlastResistant(KorppuMod.mc.level.getBlockState(pos.west()).getBlock()) ? 1 : 0);
        return (wallsX == 2 && wallsZ <= 1) || (wallsZ == 2 && wallsX <= 1);
    }

    private void checkStaircase(BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.MutableBlockPos current = pos.mutable();
            int steps = 0;
            List<AABB> staged = new ArrayList<>();

            while (isStaircaseSection(current, dir)) {
                int height = getStaircaseHeight(current);
                AABB box = new AABB(
                        current.getX(),
                        current.getY(),
                        current.getZ(),
                        current.getX() + 1,
                        current.getY() + height,
                        current.getZ() + 1
                );

                if (!staged.contains(box)) {
                    staged.add(box);
                }

                current.move(dir);
                current.move(Direction.UP);
                steps++;
            }

            if (steps >= minStaircaseLength.getValue().intValue()) {
                for (AABB box : staged) {
                    if (!staircaseBoxes.contains(box)) {
                        staircaseBoxes.add(box);
                    }
                }
            }
        }
    }

    private int getStaircaseHeight(BlockPos pos) {
        int maxHeight = maxStaircaseHeight.getValue().intValue();
        int height = 0;
        while (height < maxHeight && KorppuMod.mc.level.getBlockState(pos.above(height)).isAir()) {
            height++;
        }
        return height;
    }

    private boolean isStaircaseSection(BlockPos pos, Direction dir) {
        int height = getStaircaseHeight(pos);
        if (height < minStaircaseHeight.getValue().intValue() || height > maxStaircaseHeight.getValue().intValue()) return false;
        if (KorppuMod.mc.level.getBlockState(pos.below()).isAir()) return false;
        if (KorppuMod.mc.level.getBlockState(pos.above(height)).isAir()) return false;

        Direction[] perp = dir.getAxis() == Direction.Axis.X
                ? new Direction[]{Direction.NORTH, Direction.SOUTH}
                : new Direction[]{Direction.EAST, Direction.WEST};

        for (Direction side : perp) {
            for (int i = 0; i < height; i++) {
                if (KorppuMod.mc.level.getBlockState(pos.above(i).relative(side)).isAir()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isBlastResistant(Block block) {
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.RESPAWN_ANCHOR || block == Blocks.ENDER_CHEST || block == Blocks.NETHERITE_BLOCK;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Holes", String.valueOf(holeBoxes.size())),
                StatusEntry.text("Tunnels", String.valueOf(tunnelBoxes.size())),
                StatusEntry.text("Stairs", String.valueOf(staircaseBoxes.size())),
                StatusEntry.text("Range", String.valueOf(range.getValue().intValue()))
        );
    }
}
