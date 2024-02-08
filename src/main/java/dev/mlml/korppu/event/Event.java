package dev.mlml.korppu.event;

import lombok.Getter;
import lombok.Setter;

/**
 * This class represents an event in the system.
 * An event can be cancelled, which may prevent certain actions from occurring.
 */
@Setter
@Getter
public class Event {
    // A flag indicating whether the event has been cancelled.
    protected boolean cancelled;

    /**
     * This method is used to cancel the event.
     * Once an event is cancelled, it may prevent certain actions from occurring.
     */
    public void cancel() {
        this.cancelled = true;
    }
}
