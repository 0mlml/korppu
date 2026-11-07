package dev.mlml.korppu.item;

import java.util.Map;

public final class ItemPresetContext {
    private final Map<String, String> values;

    public ItemPresetContext(Map<String, String> values) {
        this.values = values;
    }

    public String getString(String key, String fallback) {
        String value = values.get(key);
        if (value == null) return fallback;
        value = value.trim();
        return value.isEmpty() ? fallback : value;
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public long getLong(String key, long fallback) {
        try {
            return Long.parseLong(getString(key, String.valueOf(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public float getFloat(String key, float fallback) {
        try {
            return Float.parseFloat(getString(key, String.valueOf(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(getString(key, String.valueOf(fallback)));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
