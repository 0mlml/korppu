package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Blocks;
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

public class AutoTnt extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "TNT ignition range", 4d, 1d, 8d, 0));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between ignitions", 4d, 0d, 40d, 0));

    private int cooldown;
    private int ignited;

    public AutoTnt() {
        super("AutoTnt", "Automatically ignites nearby TNT blocks", GLFW.GLFW_KEY_UNKNOWN);
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

        int igniterSlot = findIgniterSlot();
        if (igniterSlot < 0) return;

        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > range.getValue() * range.getValue()) continue;
                    if (KorppuMod.mc.level.getBlockState(pos).getBlock() != Blocks.TNT) continue;

                    if (ignite(igniterSlot, pos)) {
                        ignited++;
                        cooldown = delayTicks.getValue().intValue();
                        return;
                    }
                }
            }
        }
    }

    private boolean ignite(int slot, BlockPos target) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        BlockHitResult bhr = new BlockHitResult(target.getCenter(), Direction.UP, target, false);
        var result = KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(old);
        return result != null && result.consumesAction();
    }

    private int findIgniterSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Ignited", String.valueOf(ignited)),
                StatusEntry.text("Cooldown", String.valueOf(cooldown))
        );
    }
}
