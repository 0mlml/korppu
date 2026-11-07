package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PingSpoof extends Module {
    final List<WrappedPacket> saved = new ArrayList<>();
    final Set<Integer> sentPackets = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final DoubleSetting ping = config.add(new DoubleSetting("Ping", "Spoofed ping", 50d, 0d, 1000d, 0));
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Delay all or delay only pong", Mode.Fake));

    public PingSpoof() {
        super("PingSpoof", "Spoofs your ping", GLFW.GLFW_KEY_KP_1);

        KorppuMod.eventManager.register(this);
    }

    private int generatePacketIdentifier(Packet<?> packet) {
        return packet.hashCode();
    }

    private void sendPacket(Packet<?> packet) {
        if (!ModuleManager.isSendPackets()) {
            return;
        }

        Connection connection = Objects.requireNonNull(KorppuMod.mc.getConnection()).getConnection();

        if (connection == null) {
            return;
        }

        int packetId = generatePacketIdentifier(packet);
        sentPackets.add(packetId);

        connection.send(packet);
    }

    @Override
    public void onEnable() {
        saved.clear();
        sentPackets.clear();
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.text("Ping", ping.getValue().intValue() + " ms"),
                StatusEntry.text("Queued", saved.size() + " packets"));
    }

    @Override
    public String getStatus() {
        return String.format("%s, %dms", mode.getValue().toString().charAt(0), ping.getValue().intValue());
    }

    @Override
    public void onFastTick() {
        if (KorppuMod.mc.getConnection() == null) {
            setEnabled(false);
            return;
        }

        long curr = System.currentTimeMillis();
        for (WrappedPacket wp : saved.toArray(new WrappedPacket[0])) {
            if (wp.created + wp.delay <= curr) {
                saved.remove(wp);
                sendPacket(wp.packet);
            }
        }
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || !ModuleManager.isSendPackets()) {
            return;
        }

        if (sentPackets.contains(generatePacketIdentifier(event.getPacket()))) {
            sentPackets.remove(generatePacketIdentifier(event.getPacket()));
            return;
        }

        if (mode.getValue() == Mode.Fake && (event.getPacket() instanceof ServerboundKeepAlivePacket || event.getPacket() instanceof ServerboundPongPacket)) {
            saved.add(new WrappedPacket(event.getPacket(), ping.getValue(), System.currentTimeMillis()));
            event.setCancelled(true);
        } else {
            if (mode.getValue() == Mode.Actual) {
                saved.add(new WrappedPacket(event.getPacket(), ping.getValue(), System.currentTimeMillis()));
                event.setCancelled(true);
            }
        }
    }

    public enum Mode {
        Actual,
        Fake
    }

    record WrappedPacket(Packet<?> packet, double delay, long created) {
    }
}
