package dev.mlml.korppu.event;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Event
{
    protected boolean cancelled;

    public void cancel()
    {
        this.cancelled = true;
    }
}
