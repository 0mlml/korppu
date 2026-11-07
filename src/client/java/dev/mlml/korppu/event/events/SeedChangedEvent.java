package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.Getter;

@Getter
public class SeedChangedEvent extends Event {
    private final Long previousSeed;
    private final Long newSeed;
    private final String source;

    public SeedChangedEvent(Long previousSeed, Long newSeed, String source) {
        this.previousSeed = previousSeed;
        this.newSeed = newSeed;
        this.source = source;
    }
}
