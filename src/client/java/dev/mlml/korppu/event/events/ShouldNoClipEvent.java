package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

@Getter
@Setter
@AllArgsConstructor
public class ShouldNoClipEvent extends Event {
    final Player player;
    boolean shouldNoclip;
}
