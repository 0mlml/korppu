package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class RemoteEnderChest extends Module {
    public RemoteEnderChest() {
        super("RemoteEnderChest", "Opens your ender chest inventory remotely by sending a fake interaction packet", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        setEnabled(false); // One-shot module
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;

        var player = KorppuMod.mc.player;
        BlockPos fakePos = player.blockPosition();
        Vec3 hitVec = new Vec3(fakePos.getX() + 0.5, fakePos.getY() + 0.5, fakePos.getZ() + 0.5);

        KorppuMod.mc.getConnection().send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                new BlockHitResult(hitVec, Direction.UP, fakePos, false),
                0
        ));
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(StatusEntry.text("Mode", "Click to open EC"));
    }

    @Override
    public String getStatus() {
        return "One-shot";
    }
}
