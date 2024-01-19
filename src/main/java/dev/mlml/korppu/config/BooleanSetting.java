package dev.mlml.korppu.config;

import dev.mlml.korppu.gui.ConfigScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BooleanSetting extends GenericSetting<Boolean>
{
    public BooleanSetting(String name, String tooltip, Boolean defaultValue, List<Consumer<Boolean>> callbacks)
    {
        super(name, tooltip, defaultValue, callbacks);
    }

    public BooleanSetting(String name, String tooltip, Boolean defaultValue)
    {
        this(name, tooltip, defaultValue, Collections.emptyList());
    }

    @Override
    public Text asText()
    {
        return Text.literal(String.format("%s: %s", getName(), getValue() ? "true" : "false"));
    }

    @Override
    public ClickableWidget getAsWidget()
    {
        return ButtonWidget.builder(asText(), button ->
                {
                    setValue(!getValue());
                    callbacks.forEach(c -> c.accept(getValue()));

                    button.setMessage(asText());
                })
                .dimensions(0, 0, ConfigScreen.DEFAULT_WIDTH, ConfigScreen.DEFAULT_HEIGHT)
                .tooltip(Tooltip.of(Text.literal(getTooltip())))
                .build();
    }
}
