package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class Lavacast extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Placement scan range", 5d, 1d, 12d, 0));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between placements", 6d, 0d, 60d, 0));
    private final BooleanSetting scaffoldWater = config.add(new BooleanSetting("Scaffold Water", "Place water before lava if possible", true));

    private int cooldown;
    private int casts;

    public Lavacast() {
        super("Lavacast", "Automates water/lava placement for simple lavacasts", GLFW.GLFW_KEY_UNKNOWN);
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

        int lavaSlot = findSlot(Items.LAVA_BUCKET);
        int waterSlot = findSlot(Items.WATER_BUCKET);
        if (lavaSlot < 0) return;

        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                BlockPos support = origin.offset(x, 0, z);
                if (origin.distSqr(support) > range.getValue() * range.getValue()) continue;

                BlockPos waterPos = support.above();
                BlockPos lavaPos = support.above(2);
                BlockState supportState = KorppuMod.mc.level.getBlockState(support);
                if (supportState.isAir()) continue;
                if (!KorppuMod.mc.level.getBlockState(waterPos).canBeReplaced()) continue;
                if (!KorppuMod.mc.level.getBlockState(lavaPos).canBeReplaced()) continue;

                boolean acted = false;
                if (scaffoldWater.getValue() && waterSlot >= 0) {
                    acted = placeFromSlot(waterSlot, support, waterPos);
                }
                if (!acted) {
                    acted = placeFromSlot(lavaSlot, waterPos, lavaPos);
                }

                if (acted) {
                    casts++;
                    cooldown = delayTicks.getValue().intValue();
                    return;
                }
            }
        }
    }

    private boolean placeFromSlot(int slot, BlockPos support, BlockPos hitPos) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);

        BlockHitResult bhr = new BlockHitResult(hitPos.getCenter(), Direction.UP, support, false);
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
                StatusEntry.text("Casts", String.valueOf(casts)),
                StatusEntry.text("Cooldown", String.valueOf(cooldown)),
                StatusEntry.indicator("Scaffold Water", scaffoldWater.getValue())
        );
    }
}
