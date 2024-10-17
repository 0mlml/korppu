package dev.mlml.korppu.event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to mark methods that should be called when an event occurs.
 * Methods annotated with @Listener should be located within a class that is registered
 * to the EventManager. These methods should have a single parameter, which is a subclass of Event.
 * <p>
 * The @Listener annotation is retained at runtime, which means it can be queried at run time
 * using reflection to determine if a method should be called when an event occurs.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {
}