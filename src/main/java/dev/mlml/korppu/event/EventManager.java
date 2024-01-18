package dev.mlml.korppu.event;

import dev.mlml.korppu.KorppuMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager
{
    protected final List<Handler> handlers = new CopyOnWriteArrayList<>();

    protected void register(Handler handler)
    {
        this.handlers.add(handler);
    }

    protected List<Handler> getSubscribersByType(Class<?> subscriptionType)
    {
        return handlers.stream().filter(handler -> handler.subscriptionType.isAssignableFrom(subscriptionType)).toList();
    }

    public void send(Object ev)
    {
        try
        {
            Class<?> c = ev.getClass();
            for (Handler handler : getSubscribersByType(c))
            {
                handler.invoke(ev);
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void unregister(Class<?> i)
    {
        this.handlers.removeIf(handler -> handler.subscriptionType.isAssignableFrom(i));
    }

    public record Handler(Runnable runnable, Class<?> subscriptionType)
    {
        public void invoke(Object o) throws InvocationTargetException, IllegalAccessException
        {
            runnable.getClass().getDeclaredMethods()[0].invoke(runnable, o);
        }
    }
}