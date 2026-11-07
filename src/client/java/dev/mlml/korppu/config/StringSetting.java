package dev.mlml.korppu.config;

import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class StringSetting extends GenericSetting<String> {
    public StringSetting(String name, String tooltip, String defaultValue, List<Consumer<String>> callbacks) {
        super(name, tooltip, defaultValue, callbacks);
    }

    public StringSetting(String name, String tooltip, String defaultValue) {
        this(name, tooltip, defaultValue, Collections.emptyList());
    }

    @Override
    public String[] serialize() {
        return new String[]{"s", getName(), getValue()};
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