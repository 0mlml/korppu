package dev.mlml.korppu.config;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.ConfigScreen;
import lombok.Getter;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

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
    public Text asText() {
        return Text.literal(value);
    }

    @Override
    public ClickableWidget getAsWidget() {
        TextFieldWidget textFieldWidget = new TextFieldWidget(KorppuMod.mc.textRenderer, 0, 0, ConfigScreen.DEFAULT_WIDTH, ConfigScreen.DEFAULT_HEIGHT, asText());
        textFieldWidget.setText(value);
        textFieldWidget.setTooltip(Tooltip.of(Text.literal(tooltip)));
        textFieldWidget.setChangedListener(this::setValue);

        return textFieldWidget;
    }
}