package dev.mlml.korppu.config;

import lombok.Getter;
import net.minecraft.network.chat.Component;

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
    public Component asText() {
        return Component.literal(value.toString());
    }

}
