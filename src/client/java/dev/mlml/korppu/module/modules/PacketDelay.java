package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacketDelay extends Module {
    private final DoubleSetting delayMs = config.add(new DoubleSetting(
            "Delay", "Packet delay in milliseconds", 200d, 0d, 2000d, 0));
    private final BooleanSetting delayMovement = config.add(new BooleanSetting(
            "Movement Packets", "Delay ServerboundMovePlayerPacket", true));
    private final BooleanSetting flushOnDisable = config.add(new BooleanSetting(
            "Flush on Disable", "Send buffered packets when disabled", true));

    private record DelayedPacket(Packet<?> packet, long sendAt) {}
    private final List<DelayedPacket> buffer = new CopyOnWriteArrayList<>();
    private boolean flushing;

    public PacketDelay() {
        super("PacketDelay", "Buffers and delays outgoing packets by a configurable amount", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        flushing = false;
        if (flushOnDisable.getValue() && KorppuMod.mc.getConnection() != null) {
            flushing = true;
            try {
                for (DelayedPacket dp : buffer) {
                    KorppuMod.mc.getConnection().send(dp.packet());
                }
            } finally {
                flushing = false;
            }
        }
        buffer.clear();
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || flushing) return;
        if (!shouldDelay(event.getPacket())) return;

        event.cancel();
        long sendAt = System.currentTimeMillis() + delayMs.getValue().longValue();
        buffer.add(new DelayedPacket(event.getPacket(), sendAt));
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.getConnection() == null) {
            buffer.clear();
            return;
        }

        long now = System.currentTimeMillis();
        List<DelayedPacket> toSend = new ArrayList<>();

        for (DelayedPacket dp : buffer) {
            if (dp.sendAt() <= now) {
                toSend.add(dp);
            }
        }

        for (DelayedPacket dp : toSend) {
            buffer.remove(dp);
            flushing = true;
            try {
                KorppuMod.mc.getConnection().send(dp.packet());
            } finally {
                flushing = false;
            }
        }
    }

    private boolean shouldDelay(Packet<?> packet) {
        if (delayMovement.getValue() && packet instanceof ServerboundMovePlayerPacket) return true;
        return false;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Delay", delayMs.getValue().intValue() + " ms"),
                StatusEntry.text("Buffered packets", buffer.size() + "")
        );
    }

    @Override
    public String getStatus() {
        return delayMs.getValue().intValue() + "ms | " + buffer.size() + " queued";
    }
}
