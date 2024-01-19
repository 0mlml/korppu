package dev.mlml.korppu.event;

import java.lang.reflect.Method;
import java.util.*;

public class EventManager
{
    protected final Map<Class<?>, Set<Handler<?>>> handlers = new HashMap<>();

    public void register(Object listener)
    {
        for (Method method : listener.getClass().getDeclaredMethods())
        {
            if (isEventHandlerMethod(method))
            {
                Class<?> eventType = method.getParameterTypes()[0];
                registerHandlerMethod(listener, method, eventType);
            }
        }
    }

    private boolean isEventHandlerMethod(Method method)
    {
        return method.getParameterCount() == 1
                && Event.class.isAssignableFrom(method.getParameterTypes()[0]);
    }

    private void registerHandlerMethod(Object listener, Method method, Class<?> eventType)
    {
        Handler<?> handler = event ->
        {
            try
            {
                method.invoke(listener, event);
            } catch (Exception e)
            {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        };
        handlers.computeIfAbsent(eventType, k -> new HashSet<>() {}).add(handler);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> void trigger(E event)
    {
        Set<Handler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null)
        {
            for (Handler<?> handler : eventHandlers)
            {
                ((Handler<E>) handler).handle(event);
            }
        }
    }
}
