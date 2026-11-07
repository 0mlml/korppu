package dev.mlml.korppu.item;

public record ItemPresetPlaceholder(String key, String label, String defaultValue) {
    public ItemPresetPlaceholder {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Placeholder key cannot be blank");
    }
}
