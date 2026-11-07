package dev.mlml.korppu.module;

public class StatusEntry {
    public enum Type { TEXT, BAR, INDICATOR }

    public final String key;
    public final String value;
    public final float progress;
    public final Type type;
    public final int color;

    private StatusEntry(String key, String value, float progress, Type type, int color) {
        this.key = key;
        this.value = value;
        this.progress = progress;
        this.type = type;
        this.color = color;
    }

    public static StatusEntry text(String key, String value) {
        return new StatusEntry(key, value, 0f, Type.TEXT, -1);
    }

    public static StatusEntry text(String key, String value, int argbColor) {
        return new StatusEntry(key, value, 0f, Type.TEXT, argbColor);
    }

    public static StatusEntry bar(String key, String value, float progress) {
        return new StatusEntry(key, value, progress, Type.BAR, -1);
    }

    public static StatusEntry bar(String key, String value, float progress, int argbColor) {
        return new StatusEntry(key, value, progress, Type.BAR, argbColor);
    }

    public static StatusEntry indicator(String key, boolean active) {
        int color = active ? 0xFF55FF55 : 0xFFFF5555;
        return new StatusEntry(key, active ? "Y" : "N", 0f, Type.INDICATOR, color);
    }
}
