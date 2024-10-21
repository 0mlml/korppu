package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class AtoB extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "A to B mode", Mode.Delay));
    private final DoubleSetting maxSize = config.add(new DoubleSetting("Max Size", "Max size of the queue", 100d, 20d, 200d, 0));

    private final List<Packet<?>> queue = new ArrayList<>();

    public AtoB() {
        super("AtoB", "Drop packets from A to B", GLFW.GLFW_KEY_X);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public String getStatus() {
        switch (mode.getValue()) {
            case Delay -> {
                return "DLY: " + queue.size();
            }
            case Drop -> {
                return "DRP";
            }
        }
        return "";
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null) {
            queue.clear();
            return;
        }
        for (Packet<?> packet : queue.toArray(new Packet<?>[0])) {
            KorppuMod.mc.getNetworkHandler().sendPacket(packet);
        }
        queue.clear();
    }

    @Listener
    public void onSendPacket(PacketEvent.Sent pe) {
        if (!isEnabled()) {
            return;
        }

        if (pe.getPacket() instanceof KeepAliveC2SPacket || pe.getPacket() instanceof CommonPongC2SPacket) {
            return;
        }
        pe.cancel();
        if (mode.getValue() == Mode.Delay) {
            queue.add(pe.getPacket());
            if (queue.size() > maxSize.getValue()) {
                setEnabled(false);
            }
        }
    }

    public enum Mode {
        Delay, Drop
    }
}
