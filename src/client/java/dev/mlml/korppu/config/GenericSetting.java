package dev.mlml.korppu.config;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@Setter
public abstract class GenericSetting<V> {
    protected final String name;
    protected final String tooltip;
    final V defaultValue;
    final List<Consumer<V>> callbacks;
    V value;

    private Supplier<Boolean> visibilityCondition = null;

    public GenericSetting(String name, String tooltip, V defaultValue, List<Consumer<V>> callbacks) {
        this.name = name;
        this.tooltip = tooltip;

        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.callbacks = callbacks;
    }

    @SuppressWarnings("unchecked")
    public <Self extends GenericSetting<V>> Self showOnlyIf(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return (Self) this;
    }

    public boolean isVisible() {
        return visibilityCondition == null || visibilityCondition.get();
    }

    public String[] serialize() {
        return new String[]{"g", name, value.toString()};
    }

    @SuppressWarnings("unchecked")
    public void deserialize(String value) {
        this.value = (V) value;
    }

    public Component asText() {
        return Component.literal(String.format("%s: %s", name, value));
    }

    public void setValue(V value) {
        this.value = value;
        callbacks.forEach(c -> c.accept(value));
    }
}
