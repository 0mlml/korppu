package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.Entity;

@Getter
@AllArgsConstructor
public class EntitySpawnEvent extends Event {
    private final Entity entity;
}
