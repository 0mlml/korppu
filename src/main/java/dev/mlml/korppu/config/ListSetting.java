package dev.mlml.korppu.config;

import dev.mlml.korppu.gui.ConfigScreen;
import lombok.Getter;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ListSetting<T extends Enum<?>> extends GenericSetting<T>
{
    @Getter
    private final T[] possible;
    private final String[] possibleStrings;

    @SuppressWarnings("unchecked")
    public ListSetting(String name, String tooltip, T defaultValue, List<Consumer<T>> callbacks)
    {
        super(name, tooltip, defaultValue, callbacks);

        if (!Modifier.isPublic(defaultValue.getClass().getModifiers()))
        {
            throw new IllegalArgumentException("Enum must be public");
        }

        try
        {
            this.possible = (T[]) defaultValue.getClass().getMethod("values").invoke(null);
        } catch (Exception e)
        {
            throw new IllegalArgumentException("Enum must have a values() method");
        }

        this.possibleStrings = Arrays.stream(possible).map(Enum::name).toArray(String[]::new);

        this.label = tooltip;
    }

    public ListSetting(String name, String tooltip, T defaultValue)
    {
        this(name, tooltip, defaultValue, Collections.emptyList());
    }

    @Override
    public void setValue(T value)
    {
        if (Arrays.stream(possible).noneMatch(t -> t.equals(value)))
        {
            return;
        }
        super.setValue(value);
    }

    public void setValueFromString(String value)
    {
        setValue(Arrays.stream(possible).filter(t -> t.name().equals(value)).findFirst().orElse(getValue()));
    }

    @Override
    public Text asText()
    {
        return Text.literal(String.valueOf(getValue()));
    }

    @Override
    public ClickableWidget getAsWidget()
    {
        return CyclingButtonWidget.builder(Text::literal)
                .values(possibleStrings)
                .initially(getValue().name())
                .build(0, 0, ConfigScreen.DEFAULT_WIDTH, ConfigScreen.DEFAULT_HEIGHT, Text.literal(getName()), (button, mode) ->
                {
                    setValueFromString(mode);
                });
    }
}
