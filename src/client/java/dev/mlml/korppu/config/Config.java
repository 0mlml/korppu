package dev.mlml.korppu.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Config {
    protected final List<GenericSetting<?>> settings = new ArrayList<>();

    public void deserialize(List<String> lines) {
        Map<String, String> byName = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split(ConfigWriter.RECORD_SEPARATOR, 3);
            if (parts.length >= 3) {
                byName.put(parts[1], parts[2]);
            }
        }

        for (GenericSetting<?> setting : settings) {
            String value = byName.get(setting.getName());
            if (value != null) {
                setting.deserialize(value);
            }
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

}
