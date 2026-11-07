package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ObsidianFarm extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Target search range", 4d, 1d, 10d, 0));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between actions", 5d, 0d, 40d, 0));
    private final DoubleSetting settleTicks = config.add(new DoubleSetting("Settle", "Ticks to wait for lava->obsidian conversion", 6d, 0d, 60d, 0));
    private final BooleanSetting recoverWater = config.add(new BooleanSetting("Recover Water", "Try to pick up placed water with empty bucket", true));

    private int cooldown;
    private int mined;
    private int converted;
    private Phase phase = Phase.SEARCH_LAVA;
    private BlockPos activePos;

    public ObsidianFarm() {
        super("ObsidianFarm", "Places water on lava and mines resulting obsidian", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
        phase = Phase.SEARCH_LAVA;
        activePos = null;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        switch (phase) {
            case SEARCH_LAVA -> searchAndPlaceWater();
            case WAIT_CONVERSION -> waitForConversion();
            case MINE_OBSIDIAN -> mineConverted();
            case RECOVER_WATER -> recoverPlacedWater();
        }
    }

    private void searchAndPlaceWater() {
        int waterSlot = findSlot(Items.WATER_BUCKET);
        if (waterSlot < 0) {
            mineNearbyObsidian();
            return;
        }

        BlockPos lava = findNearby(Blocks.LAVA);
        if (lava == null) {
            mineNearbyObsidian();
            return;
        }
        if (!KorppuMod.mc.level.getBlockState(lava.above()).canBeReplaced()) return;

        if (useBucketOn(waterSlot, lava)) {
            activePos = lava.immutable();
            phase = Phase.WAIT_CONVERSION;
            cooldown = settleTicks.getValue().intValue();
        }
    }

    private void waitForConversion() {
        if (activePos == null) {
            phase = Phase.SEARCH_LAVA;
            return;
        }

        if (KorppuMod.mc.level.getBlockState(activePos).getBlock() == Blocks.OBSIDIAN) {
            converted++;
            phase = Phase.MINE_OBSIDIAN;
            cooldown = 1;
            return;
        }

        if (KorppuMod.mc.level.getBlockState(activePos).getBlock() != Blocks.LAVA) {
            phase = Phase.SEARCH_LAVA;
            activePos = null;
            return;
        }

        cooldown = Math.max(1, settleTicks.getValue().intValue() / 2);
    }

    private void mineConverted() {
        if (activePos == null) {
            phase = Phase.SEARCH_LAVA;
            return;
        }

        if (KorppuMod.mc.level.getBlockState(activePos).getBlock() == Blocks.OBSIDIAN) {
            mine(activePos);
            mined++;
        }

        if (recoverWater.getValue()) {
            phase = Phase.RECOVER_WATER;
        } else {
            phase = Phase.SEARCH_LAVA;
            activePos = null;
        }
        cooldown = delayTicks.getValue().intValue();
    }

    private void recoverPlacedWater() {
        if (activePos == null) {
            phase = Phase.SEARCH_LAVA;
            return;
        }

        int emptySlot = findSlot(Items.BUCKET);
        if (emptySlot < 0) {
            phase = Phase.SEARCH_LAVA;
            activePos = null;
            return;
        }

        BlockPos waterPos = activePos.above();
        if (KorppuMod.mc.level.getBlockState(waterPos).getBlock() == Blocks.WATER) {
            useBucketOn(emptySlot, waterPos);
        }

        phase = Phase.SEARCH_LAVA;
        activePos = null;
        cooldown = 2;
    }

    private void mineNearbyObsidian() {
        BlockPos obsidian = findNearby(Blocks.OBSIDIAN);
        if (obsidian == null) return;

        mine(obsidian);
        mined++;
        cooldown = delayTicks.getValue().intValue();
    }

    private BlockPos findNearby(net.minecraft.world.level.block.Block block) {
        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > range.getValue() * range.getValue()) continue;
                    if (KorppuMod.mc.level.getBlockState(pos).getBlock() == block) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private void mine(BlockPos pos) {
        KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private boolean useBucketOn(int slot, BlockPos targetPos) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        BlockHitResult bhr = new BlockHitResult(targetPos.getCenter(), Direction.UP, targetPos, false);
        var result = KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(old);
        return result != null && result.consumesAction();
    }

    private int findSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) return i;
        }
        return -1;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Mined", String.valueOf(mined)),
                StatusEntry.text("Converted", String.valueOf(converted)),
                StatusEntry.text("Phase", phase.name()),
                StatusEntry.text("Cooldown", String.valueOf(cooldown))
        );
    }

    private enum Phase {
        SEARCH_LAVA,
        WAIT_CONVERSION,
        MINE_OBSIDIAN,
        RECOVER_WATER
    }
}
