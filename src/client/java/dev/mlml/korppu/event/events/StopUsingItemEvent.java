package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@Getter
@AllArgsConstructor
public class StopUsingItemEvent extends Event {
    LivingEntity entity;
    ItemStack stack;
}
