package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChestAura extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Search range for containers", 4d, 1d, 8d, 1));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between interactions", 6d, 0d, 40d, 0));
    private final BooleanSetting includeBarrels = config.add(new BooleanSetting("Include Barrels", "Target barrels in addition to chests", true));
    private final BooleanSetting includeTrapped = config.add(new BooleanSetting("Include Trapped", "Target trapped chests", true));
    private final BooleanSetting closeOpened = config.add(new BooleanSetting("Close Opened", "Close opened container screen automatically", true));
    private final BooleanSetting oncePerChest = config.add(new BooleanSetting("Once Per Chest", "Only open each chest once per enable", true));

    private int cooldown;
    private final Set<BlockPos> opened = new HashSet<>();

    public ChestAura() {
        super("ChestAura", "Auto-opens nearby chests and barrels", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        cooldown = 0;
        opened.clear();
    }

    @Override
    public void onDisable() {
        opened.clear();
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) return;

        if (closeOpened.getValue() && KorppuMod.mc.screen instanceof AbstractContainerScreen<?>) {
            KorppuMod.mc.player.closeContainer();
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > range.getValue() * range.getValue()) continue;
                    if (oncePerChest.getValue() && opened.contains(pos)) continue;

                    Block block = KorppuMod.mc.level.getBlockState(pos).getBlock();
                    if (!isTarget(block)) continue;

                    Vec3 hit = pos.getCenter();
                    BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, pos, false);
                    KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
                    KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);

                    opened.add(pos.immutable());
                    cooldown = delayTicks.getValue().intValue();
                    return;
                }
            }
        }
    }

    private boolean isTarget(Block block) {
        if (block instanceof ChestBlock) {
            return includeTrapped.getValue() || !(block instanceof TrappedChestBlock);
        }
        return includeBarrels.getValue() && block instanceof BarrelBlock;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Opened", String.valueOf(opened.size())),
                StatusEntry.text("Cooldown", String.valueOf(cooldown)),
                StatusEntry.indicator("Barrels", includeBarrels.getValue()),
                StatusEntry.indicator("Trapped", includeTrapped.getValue())
        );
    }
}
