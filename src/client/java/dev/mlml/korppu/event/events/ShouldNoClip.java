package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;

@Getter
@Setter
@AllArgsConstructor
public class ShouldNoClip extends Event {
    final PlayerEntity player;
    boolean shouldNoclip;
}
