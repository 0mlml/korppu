package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;

@Getter
public class PlayedSoundEvent extends Event {
    private final Vec3 position;
    private final String soundId;

    public PlayedSoundEvent(Vec3 position, String soundId) {
        this.position = position;
        this.soundId = soundId;
    }
}
