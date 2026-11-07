package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class NoFall extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<Mode>("Mode", "Method", Mode.Packet));
    private final DoubleSetting fallDistance = config.add(new DoubleSetting("Fall Distance", "The distance you can fall before triggering", 2.0, 1.0, 10.0, 1));
    private boolean hasSentOnGround = false;

    public NoFall() {
        super("NoFall", "Prevents fall damage", GLFW.GLFW_KEY_N);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new java.util.ArrayList<>();
        entries.add(StatusEntry.text("Mode", mode.getValue().name()));
        if (KorppuMod.mc.player != null) {
            float dist = (float) KorppuMod.mc.player.fallDistance;
            float threshold = fallDistance.getValue().floatValue();
            int color = dist >= threshold ? 0xFFFF5555 : 0xFF55FF55;
            entries.add(StatusEntry.bar("Fall",
                    String.format("%.1f / %.1f", dist, threshold),
                    Math.min(1f, dist / Math.max(0.1f, threshold)),
                    color));
        }
        return entries;
    }

    @Override
    public String getStatus() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null || KorppuMod.mc.player.fallDistance <= 0.1f) {
            hasSentOnGround = false;
            return mode.getValue().name();
        }

        String distanceString = String.format("%.2f", KorppuMod.mc.player.fallDistance);
        TextFormatter.Code color = KorppuMod.mc.player.fallDistance - fallDistance.getValue() < 0 ? TextFormatter.Code.GREEN : TextFormatter.Code.RED;
        if (hasSentOnGround && color == TextFormatter.Code.RED) {
            color = TextFormatter.Code.YELLOW;
        }

        return mode.getValue().name() + " " + TextFormatter.format("%1%s", color, distanceString);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        if (KorppuMod.mc.player.fallDistance > fallDistance.getValue()) {
            switch (mode.getValue()) {
                case Packet -> {
                    KorppuMod.mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                    hasSentOnGround = true;
                }
                case Velocity -> {
                    KorppuMod.mc.player.setDeltaMovement(0, 0.1, 0);
                    KorppuMod.mc.player.fallDistance = 0;
                }
            }
        }
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent packetEvent) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        if (packetEvent.getPacket() instanceof ServerboundMovePlayerPacket && mode.getValue() == Mode.OnGround) {
            ((IPlayerMoveC2SPacketMixin) packetEvent.getPacket()).setOnGround(true);
            hasSentOnGround = true;
        }
    }

    public enum Mode {
        Packet,
        OnGround,
        Velocity
    }
}

