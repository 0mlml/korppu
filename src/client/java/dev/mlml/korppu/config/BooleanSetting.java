package dev.mlml.korppu.config;

import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BooleanSetting extends GenericSetting<Boolean> {
    public BooleanSetting(String name, String tooltip, Boolean defaultValue, List<Consumer<Boolean>> callbacks) {
        super(name, tooltip, defaultValue, callbacks);
    }

    public BooleanSetting(String name, String tooltip, Boolean defaultValue) {
        this(name, tooltip, defaultValue, Collections.emptyList());
    }

    @Override
    public String[] serialize() {
        return new String[]{"b", getName(), getValue().toString()};
    }

    @Override
    public void deserialize(String value) {
        setValue(Boolean.parseBoolean(value));
    }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
        getCallbacks().forEach(c -> c.accept(value));
    }

    @Override
    public Component asText() {
        return Component.literal(String.format("%s: %s", getName(), getValue() ? "true" : "false"));
    }

}
