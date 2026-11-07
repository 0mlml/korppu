package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.Getter;

import java.util.UUID;

/**
 * Fired when a player entity enters or leaves the local client's render distance.
 */
@Getter
public abstract class PlayerTrackEvent extends Event {
    private final String playerName;
    private final UUID playerId;

    protected PlayerTrackEvent(String playerName, UUID playerId) {
        this.playerName = playerName;
        this.playerId = playerId;
    }

    public static class Enter extends PlayerTrackEvent {
        public Enter(String playerName, UUID playerId) {
            super(playerName, playerId);
        }
    }

    public static class Leave extends PlayerTrackEvent {
        public Leave(String playerName, UUID playerId) {
            super(playerName, playerId);
        }
    }
}
