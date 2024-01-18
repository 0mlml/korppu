package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.Packet;

@Getter
@Setter
@AllArgsConstructor
public class PacketEvent extends Event
{
    Packet<?> packet;
    Type type;

    public enum Type
    {
        INBOUND,
        OUTBOUND
    }
}
