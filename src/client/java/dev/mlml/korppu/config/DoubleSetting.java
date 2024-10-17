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
public class DoubleSetting extends GenericSetting<Double> {
    private final Double min;
    private final Double max;
    private final int precision;

    public DoubleSetting(String name, String tooltip, Double defaultValue, Double min, Double max, int precision, List<Consumer<Double>> callbacks) {
        super(name, tooltip, defaultValue, callbacks);

        this.min = min;
        this.max = max;
        this.precision = precision;
    }

    public DoubleSetting(String name, String tooltip, Double defaultValue, Double min, Double max, int precision) {
        this(name, tooltip, defaultValue, min, max, precision, Collections.emptyList());
    }

    @Override
    public String[] serialize() {
        return new String[]{"d", getName(), getValue().toString()};
    }

    @Override
    public void deserialize(String value) {
        double val = Double.parseDouble(value);

        if (val < min) {
            val = min;
        } else {
            if (val > max) {
                val = max;
            }
        }

        setValue(val);
    }

    public void setValueFromString(String s) {
        try {
            double val = Double.parseDouble(s);

            val = Math.round(val * Math.pow(10, precision)) / Math.pow(10, precision);

            if (val < min) {
                val = min;
            } else {
                if (val > max) {
                    val = max;
                }
            }

            setValue(val);
        } catch (Exception e) {
            setValue(getDefaultValue());
        }

        setValue(value);
    }

    @Override
    public Text asText() {
        return Text.literal(value.toString());
    }

    @Override
    public ClickableWidget getAsWidget() {
        TextFieldWidget textFieldWidget = new TextFieldWidget(KorppuMod.mc.textRenderer, 0, 0, ConfigScreen.DEFAULT_WIDTH, ConfigScreen.DEFAULT_HEIGHT, asText());
        textFieldWidget.setTextPredicate(s -> s.matches("^[0-9]*.?[0-9]*$"));
        textFieldWidget.setText(value.toString());
        textFieldWidget.setTooltip(Tooltip.of(Text.literal(tooltip)));
        textFieldWidget.setChangedListener(this::setValueFromString);

        return textFieldWidget;
    }
}
