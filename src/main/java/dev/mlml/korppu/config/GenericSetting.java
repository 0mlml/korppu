package dev.mlml.korppu.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

public abstract class GenericSetting<V>
{
    @Getter
    public final String name;
    @Getter
    public final String tooltip;

    @Getter
    final V defaultValue;
    final List<Consumer<V>> callbacks;

    @Getter
    @Setter
    V value;

    public GenericSetting(String name, String tooltip, V defaultValue, List<Consumer<V>> callbacks)
    {
        this.name = name;
        this.tooltip = tooltip;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.callbacks = callbacks;
    }

    public String asString()
    {
        return getValue().toString();
    }
}
