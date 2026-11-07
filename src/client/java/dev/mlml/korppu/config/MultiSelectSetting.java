package dev.mlml.korppu.config;

import lombok.Getter;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class MultiSelectSetting extends GenericSetting<Set<String>> {
    private final Supplier<List<String>> optionsSupplier;
    @Getter
    private final boolean searchable;
    @Getter
    private final boolean allowCustom;
    private List<String> cachedOptions;

    public MultiSelectSetting(String name, String tooltip, Supplier<List<String>> optionsSupplier,
                               boolean searchable, boolean allowCustom) {
        super(name, tooltip, new LinkedHashSet<>(), Collections.emptyList());
        this.optionsSupplier = optionsSupplier;
        this.searchable = searchable;
        this.allowCustom = allowCustom;
    }

    public MultiSelectSetting(String name, String tooltip, Supplier<List<String>> optionsSupplier,
                               boolean searchable, boolean allowCustom, Set<String> initialSelected) {
        super(name, tooltip, new LinkedHashSet<>(initialSelected), Collections.emptyList());
        this.value = new LinkedHashSet<>(initialSelected);
        this.optionsSupplier = optionsSupplier;
        this.searchable = searchable;
        this.allowCustom = allowCustom;
    }

    public List<String> getOptions() {
        if (cachedOptions == null) {
            cachedOptions = optionsSupplier.get();
        }
        return cachedOptions;
    }

    public boolean isSelected(String option) {
        return value.contains(option);
    }

    public void toggle(String option) {
        Set<String> current = getValue();
        if (current.contains(option)) current.remove(option);
        else current.add(option);
        setValue(current);
    }

    @Override
    public String[] serialize() {
        return new String[]{"ms", getName(), String.join(ConfigWriter.UNIT_SEPARATOR, value)};
    }

    @Override
    public void deserialize(String serialized) {
        value = new LinkedHashSet<>();
        if (!serialized.isEmpty()) {
            for (String s : serialized.split(Pattern.quote(ConfigWriter.UNIT_SEPARATOR))) {
                if (!s.isEmpty()) value.add(s);
            }
        }
    }
}
