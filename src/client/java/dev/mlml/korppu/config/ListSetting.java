package dev.mlml.korppu.config;

import dev.mlml.korppu.KorppuMod;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ListSetting<T extends Enum<?>> extends GenericSetting<T> {
    @Getter
    private final T[] possible;
    private final String[] possibleStrings;

    @SuppressWarnings("unchecked")
    public ListSetting(String name, String tooltip, T defaultValue, List<Consumer<T>> callbacks) {
        super(name, tooltip, defaultValue, callbacks);

        if (!Modifier.isPublic(defaultValue.getDeclaringClass().getModifiers())) {
            throw new IllegalArgumentException("Enum must be public");
        }

        try {
            this.possible = (T[]) defaultValue.getDeclaringClass().getMethod("values").invoke(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Enum must have a values() method");
        }

        this.possibleStrings = Arrays.stream(possible).map(Enum::name).toArray(String[]::new);
    }

    public ListSetting(String name, String tooltip, T defaultValue) {
        this(name, tooltip, defaultValue, Collections.emptyList());
    }

    @Override
    public String[] serialize() {
        return new String[]{"l", getName(), getValue().name()};
    }

    @Override
    public void deserialize(String value) {
        if (Arrays.stream(possible).noneMatch(t -> t.name().equals(value))) {
            KorppuMod.LOGGER.warn("Invalid value for {}: {}", name, value);
            return;
        }

        setValueFromString(value);
    }

    @Override
    public void setValue(T value) {
        if (Arrays.stream(possible).noneMatch(t -> t.equals(value))) {
            return;
        }
        super.setValue(value);
    }

    public void setValueFromString(String value) {
        setValue(Arrays.stream(possible).filter(t -> t.name().equals(value)).findFirst().orElse(getValue()));
    }

    @Override
    public Component asText() {
        return Component.literal(String.valueOf(getValue()));
    }

}
