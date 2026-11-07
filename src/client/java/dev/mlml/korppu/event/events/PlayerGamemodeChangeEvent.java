package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.level.GameType;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PlayerGamemodeChangeEvent extends Event {
    private final String playerName;
    private final UUID playerId;
    private final GameType oldGameMode;
    private final GameType newGameMode;
}
