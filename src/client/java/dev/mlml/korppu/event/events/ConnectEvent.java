package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;


public abstract class ConnectEvent extends Event {
    public static class Joined extends ConnectEvent {
        public Joined() {
        }
    }

    public static class Left extends ConnectEvent {
        public Left() {
        }
    }
}