package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatSendEvent extends Event {
    String message;
}
