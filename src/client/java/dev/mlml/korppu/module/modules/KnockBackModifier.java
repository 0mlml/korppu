package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KnockBackModifier extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "How to modify knockback", Mode.Reduce));
    private final DoubleSetting multiplier = config.add(new DoubleSetting(
            "Multiplier", "Knockback velocity multiplier (0 = cancel, 1 = normal, 2 = double)", 0.1d, 0d, 3d, 2));

    public KnockBackModifier() {
        super("KnockBackModifier", "Modifies knockback velocity received from the server", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Listener
    public void onPacketReceived(PacketEvent.Received event) {
        if (KorppuMod.mc.player == null) return;
        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket packet)) return;
        if (packet.id() != KorppuMod.mc.player.getId()) return;

        switch (mode.getValue()) {
            case Cancel -> event.cancel();
            case Reduce, Multiply -> {
                double mult = multiplier.getValue();
                // ClientboundSetEntityMotionPacket velocity fields are read-only,
                // so we cancel and apply modified velocity directly to the player
                event.cancel();
                var vel = packet.movement();
                KorppuMod.mc.player.setDeltaMovement(vel.x * mult, vel.y * mult, vel.z * mult);
            }
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.bar("Multiplier", String.format("%.2f×", multiplier.getValue()),
                        (float) (multiplier.getValue() / 3.0))
        );
    }

    @Override
    public String getStatus() {
        return mode.getValue().name() + " " + String.format("%.2f×", multiplier.getValue());
    }

    public enum Mode {
        Cancel, Reduce, Multiply
    }
}
