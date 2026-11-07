package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ShulkerDupe extends Module {
    private final DoubleSetting packetsPerTick = config.add(new DoubleSetting(
            "Packets/Tick", "Break/use packet rounds each tick", 8d, 1d, 100d, 0));
    private final BooleanSetting requireCrosshair = config.add(new BooleanSetting(
            "Require Crosshair", "Only target the currently looked-at block", true));

    private int rounds;

    public ShulkerDupe() {
        super("ShulkerDupe", "Sends rapid break/use packets to shulker boxes", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;

        BlockPos target = findTarget();
        if (target == null) return;

        BlockHitResult bhr = new BlockHitResult(target.getCenter(), Direction.UP, target, false);
        int count = packetsPerTick.getValue().intValue();

        for (int i = 0; i < count; i++) {
            KorppuMod.mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bhr, i));
            KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, target, Direction.UP));
            KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, target, Direction.UP));
            rounds++;
        }
    }

    private BlockPos findTarget() {
        if (requireCrosshair.getValue()) {
            if (!(KorppuMod.mc.hitResult instanceof BlockHitResult bhr)) return null;
            BlockPos pos = bhr.getBlockPos();
            if (KorppuMod.mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                return pos;
            }
            return null;
        }

        BlockPos origin = KorppuMod.mc.player.blockPosition();
        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (KorppuMod.mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
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
                StatusEntry.text("Rounds", String.valueOf(rounds)),
                StatusEntry.text("Per Tick", String.valueOf(packetsPerTick.getValue().intValue())),
                StatusEntry.indicator("Crosshair", requireCrosshair.getValue())
        );
    }
}
