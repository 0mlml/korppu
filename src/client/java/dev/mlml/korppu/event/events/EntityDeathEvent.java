package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

@Getter
@AllArgsConstructor
public class EntityDeathEvent extends Event {
    private final LivingEntity entity;
    private final DamageSource source;
}
