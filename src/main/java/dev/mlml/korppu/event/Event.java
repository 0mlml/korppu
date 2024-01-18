package dev.mlml.korppu.event;

import lombok.Getter;
import lombok.Setter;

public class Event
{
    @Getter
    @Setter
    protected boolean cancelled;

    public void cancel()
    {
        this.cancelled = true;
    }
}
