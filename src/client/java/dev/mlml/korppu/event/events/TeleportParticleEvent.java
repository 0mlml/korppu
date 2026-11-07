package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;

@Getter
public class TeleportParticleEvent extends Event {
    private final Vec3 position;
    private final String particleId;

    public TeleportParticleEvent(Vec3 position, String particleId) {
        this.position = position;
        this.particleId = particleId;
    }
}
