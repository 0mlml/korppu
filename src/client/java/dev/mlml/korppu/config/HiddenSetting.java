package dev.mlml.korppu.config;

import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class HiddenSetting extends GenericSetting<String> {
    public HiddenSetting(String name, String defaultValue, List<Consumer<String>> callbacks) {
        super(name, "", defaultValue, callbacks);
    }

    public HiddenSetting(String name, String defaultValue) {
        this(name, defaultValue, Collections.emptyList());
    }

    @Override
    public String[] serialize() {
        return new String[]{"h", getName(), getValue()};
    }

    @Override
    public void deserialize(String value) {
        setValue(value);
    }

    @Override
    public Component asText() {
        return Component.literal(value);
    }

}
