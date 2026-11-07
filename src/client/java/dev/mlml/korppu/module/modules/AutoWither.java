package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoWither extends Module {
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between placement attempts", 3d, 0d, 20d, 0));
    private final BooleanSetting flatOnly = config.add(new BooleanSetting("Flat Only", "Only build if base area is flat", true));

    private int cooldown;
    private int built;

    public AutoWither() {
        super("AutoWither", "Builds wither structure automatically in front of you", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Direction facing = KorppuMod.mc.player.getDirection();
        BlockPos center = KorppuMod.mc.player.blockPosition().relative(facing, 2);

        BlockPos soul1 = center;
        BlockPos soul2 = center.above();
        BlockPos soul3 = center.above(2);
        BlockPos armL = soul3.relative(facing.getCounterClockWise());
        BlockPos armR = soul3.relative(facing.getClockWise());

        if (flatOnly.getValue() && !isFlatBase(soul1)) return;

        int soulSlot = findSlot(Items.SOUL_SAND, Items.SOUL_SOIL);
        int skullSlot = findSlot(Items.WITHER_SKELETON_SKULL);
        if (soulSlot < 0 || skullSlot < 0) return;

        if (tryPlace(soulSlot, soul1.below(), soul1)
                || tryPlace(soulSlot, soul2.below(), soul2)
                || tryPlace(soulSlot, soul3.below(), soul3)
                || tryPlace(soulSlot, armL.below(), armL)
                || tryPlace(soulSlot, armR.below(), armR)
                || tryPlace(skullSlot, soul3, soul3.above())
                || tryPlace(skullSlot, armL, armL.above())
                || tryPlace(skullSlot, armR, armR.above())) {
            cooldown = delayTicks.getValue().intValue();
            built++;
        }
    }

    private boolean isFlatBase(BlockPos root) {
        int y = root.getY() - 1;
        BlockState a = KorppuMod.mc.level.getBlockState(new BlockPos(root.getX(), y, root.getZ()));
        BlockState b = KorppuMod.mc.level.getBlockState(new BlockPos(root.getX() + 1, y, root.getZ()));
        BlockState c = KorppuMod.mc.level.getBlockState(new BlockPos(root.getX() - 1, y, root.getZ()));
        return !a.isAir() && !b.isAir() && !c.isAir();
    }

    private boolean tryPlace(int slot, BlockPos support, BlockPos place) {
        if (!KorppuMod.mc.level.getBlockState(place).canBeReplaced()) return false;

        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);

        BlockHitResult bhr = new BlockHitResult(place.getCenter(), Direction.UP, support, false);
        var result = KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(old);

        return result != null && result.consumesAction();
    }

    private int findSlot(net.minecraft.world.item.Item... items) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            for (var item : items) {
                if (stack.getItem() == item) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Actions", String.valueOf(built)),
                StatusEntry.text("Cooldown", String.valueOf(cooldown)),
                StatusEntry.indicator("Flat Only", flatOnly.getValue())
        );
    }
}
