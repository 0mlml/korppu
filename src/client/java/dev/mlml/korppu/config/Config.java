package dev.mlml.korppu.config;

import lombok.Getter;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Config {
    protected final List<GenericSetting<?>> settings = new ArrayList<>();

    public void deserialize(List<String> lines) {
        Map<String, String> byName = lines.stream()
                                          .collect(Collectors.toMap(s -> s.split(ConfigWriter.RECORD_SEPARATOR)[1], s -> s));

        for (GenericSetting<?> setting : settings) {
            if (!byName.containsKey(setting.getName())) {
                continue;
            }

            setting.deserialize(byName.get(setting.getName()).split(ConfigWriter.RECORD_SEPARATOR)[2]);
        }
    }

    public void deserialize(String serialized) {
        deserialize(List.of(serialized.split(ConfigWriter.GROUP_SEPARATOR)));
    }

    public String serialize() {
        return settings.stream()
                       .map(GenericSetting::serialize)
                       .map(strings -> String.join(ConfigWriter.RECORD_SEPARATOR, strings))
                       .collect(Collectors.joining(ConfigWriter.GROUP_SEPARATOR));
    }

    public <S extends GenericSetting<?>> S add(S ta) {
        settings.add(ta);
        return ta;
    }

    @SuppressWarnings("unchecked")
    public <S extends GenericSetting<?>> S get(String name) {
        for (GenericSetting<?> setting : settings) {
            if (setting.getName().equals(name)) {
                return (S) setting;
            }
        }

        return null;
    }

    public List<ClickableWidget> getAsWidgets() {
        List<ClickableWidget> widgets = new ArrayList<>();

        for (GenericSetting<?> setting : settings) {
            widgets.add(setting.getAsWidget());
        }

        return widgets;
    }
}
