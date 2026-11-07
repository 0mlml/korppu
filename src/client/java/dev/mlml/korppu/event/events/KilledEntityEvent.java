package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.LivingEntity;

@Getter
@Setter
@AllArgsConstructor
public class KilledEntityEvent extends Event {
    LivingEntity victim;
    LivingEntity adversary;
}
