package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.StringSetting;
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

public class HandOfGod extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Payload strategy", Mode.PacketInteract));
    private final StringSetting target = config.add(new StringSetting("Target", "Target selector or player name for command mode", "@a")
            .showOnlyIf(() -> mode.getValue() == Mode.Command));
    private final DoubleSetting actionsPerTick = config.add(new DoubleSetting("Actions/Tick", "Payload actions to send each tick", 2d, 1d, 50d, 0));
    private final DoubleSetting interactRange = config.add(new DoubleSetting("Interact Range", "Packet-interact distance in look direction", 128d, 8d, 1024d, 0)
            .showOnlyIf(() -> mode.getValue() == Mode.PacketInteract));
    private final DoubleSetting interactYOffset = config.add(new DoubleSetting("Interact Y Offset", "Vertical offset applied to packet-interact target", 0d, -64d, 64d, 0)
            .showOnlyIf(() -> mode.getValue() == Mode.PacketInteract));

    private int sent;

    public HandOfGod() {
        super("HandOfGod", "Sends repeated command payloads at a target", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;

        int count = actionsPerTick.getValue().intValue();
        if (mode.getValue() == Mode.Command) {
            String tgt = target.getValue().isBlank() ? "@a" : target.getValue().trim();
            for (int i = 0; i < count; i++) {
                KorppuMod.mc.player.connection.sendCommand("execute at " + tgt + " run summon lightning_bolt ~ ~ ~");
                sent++;
            }
            return;
        }

        Vec3 eye = KorppuMod.mc.player.getEyePosition();
        Vec3 dir = KorppuMod.mc.player.getViewVector(0).normalize();
        Vec3 targetPos = eye.add(dir.scale(interactRange.getValue())).add(0, interactYOffset.getValue(), 0);
        BlockPos blockPos = BlockPos.containing(targetPos);
        BlockHitResult bhr = new BlockHitResult(targetPos, Direction.UP, blockPos, false);

        for (int i = 0; i < count; i++) {
            KorppuMod.mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bhr, i));
            sent++;
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.text("Target", mode.getValue() == Mode.Command ? target.getValue() : "lookvec"),
                StatusEntry.text("Sent", String.valueOf(sent)),
                StatusEntry.text("Per Tick", String.valueOf(actionsPerTick.getValue().intValue()))
        );
    }

    public enum Mode {
        PacketInteract,
        Command
    }
}
