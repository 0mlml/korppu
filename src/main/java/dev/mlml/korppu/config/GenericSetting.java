package dev.mlml.korppu.config;

import dev.mlml.korppu.gui.ConfigScreen;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

@Getter
@Setter
public abstract class GenericSetting<V> {
    protected final String name;
    protected final String tooltip;
    final V defaultValue;
    final List<Consumer<V>> callbacks;
    protected String label;
    @Setter
    V value;

    public GenericSetting(String name, String tooltip, V defaultValue, List<Consumer<V>> callbacks) {
        this.name = name;
        this.tooltip = tooltip;
        this.label = name;

        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.callbacks = callbacks;
    }

    public String[] serialize() {
        return new String[]{"g", name, value.toString()};
    }

    @SuppressWarnings("unchecked")
    public void deserialize(String value) {
        this.value = (V) value;
    }

    public Text asText() {
        return Text.literal(String.format("%s: %s", name, value));
    }

    public ClickableWidget getAsWidget() {
        return ButtonWidget.builder(Text.literal(name), button -> {
                               System.out.printf("%s clicked%n", name);
                           })
                           .dimensions(0, 0, ConfigScreen.DEFAULT_WIDTH, ConfigScreen.DEFAULT_HEIGHT)
                           .tooltip(Tooltip.of(Text.literal(tooltip)))
                           .build();
    }
}
