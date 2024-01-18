package dev.mlml.korppu.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventManager
{
    private static final Map<Class<? extends Event>, List<Consumer<Event>>> listeners = new HashMap<>();

    public static void register(Class<? extends Event> eventClass, Consumer<Event> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    public static void fire(Event event) {
        List<Consumer<Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<Event> listener : eventListeners) {
                listener.accept(event);
            }
        }
    }
}
