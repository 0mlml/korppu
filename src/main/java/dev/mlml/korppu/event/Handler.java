package dev.mlml.korppu.event;

@FunctionalInterface
public interface Handler<E extends Event>
{
    void handle(E event);
}
