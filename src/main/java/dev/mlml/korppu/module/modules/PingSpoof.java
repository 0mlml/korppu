package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PingSpoof extends Module
{
    final List<WrappedPacket> saved = new ArrayList<>();
    final List<Integer> sentPackets = new ArrayList<>();

    private final DoubleSetting ping = config.add(new DoubleSetting("Ping", "Spoofed ping", 50d, 0d, 1000d, 0));
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Delay all or delay only pong", Mode.Fake));

    public PingSpoof()
    {
        super("PingSpoof", "Spoofs your ping", ModuleType.META, GLFW.GLFW_KEY_KP_1);

        PingSpoofPacketHandler packetHandler = new PingSpoofPacketHandler();
        KorppuMod.eventManager.register(packetHandler);
    }

    private int generatePacketIdentifier(Packet<?> packet)
    {
        return packet.hashCode();
    }

    private void sendPacket(Packet<?> packet)
    {
        if (!ModuleManager.isSendPackets())
        {
            return;
        }

        ClientConnection connection = Objects.requireNonNull(KorppuMod.mc.getNetworkHandler()).getConnection();

        if (connection == null)
        {
            return;
        }

        int packetId = generatePacketIdentifier(packet);
        sentPackets.add(packetId);

        connection.send(packet);
    }

    @Override
    public void onEnable()
    {
        saved.clear();
        sentPackets.clear();
    }

    @Override
    public void onFastTick()
    {
        long curr = System.currentTimeMillis();
        for (WrappedPacket wp : saved)
        {
            if (wp.created + wp.delay <= curr)
            {
                saved.remove(wp);
                sendPacket(wp.packet);
            }
        }
    }

    public enum Mode
    {
        Actual,
        Fake
    }

    record WrappedPacket(Packet<?> packet, double delay, long created)
    {

    }

    public class PingSpoofPacketHandler
    {
        public void onPacketSend(PacketEvent.Sent event)
        {
            if (!isEnabled() || !ModuleManager.isSendPackets())
            {
                return;
            }

            if (sentPackets.contains(generatePacketIdentifier(event.getPacket())))
            {
                sentPackets.remove(generatePacketIdentifier(event.getPacket()));
                return;
            }

            if (mode.getValue() == Mode.Fake && (event.getPacket() instanceof KeepAliveC2SPacket || event.getPacket() instanceof CommonPongC2SPacket))
            {
                saved.add(new WrappedPacket(event.getPacket(), ping.getValue(), System.currentTimeMillis()));
                event.setCancelled(true);
            } else
            {
                if (mode.getValue() == Mode.Actual)
                {
                    saved.add(new WrappedPacket(event.getPacket(), ping.getValue(), System.currentTimeMillis()));
                    event.setCancelled(true);
                }
            }

        }
    }
}
