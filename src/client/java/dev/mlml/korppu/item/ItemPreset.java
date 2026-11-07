package dev.mlml.korppu.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ItemPreset {
    private final String name;
    private final String description;
    private final List<ItemPresetPlaceholder> placeholders;
    private final ItemPresetGenerator generator;

    public ItemPreset(String name, String description, Supplier<ItemStack> supplier) {
        this(name, description, List.of(), ctx -> supplier.get());
    }

    public ItemPreset(String name, String description, ItemPresetGenerator generator) {
        this(name, description, List.of(), generator);
    }

    public ItemPreset(String name, String description, List<ItemPresetPlaceholder> placeholders, ItemPresetGenerator generator) {
        this.name = name;
        this.description = description;
        this.placeholders = placeholders == null ? List.of() : List.copyOf(placeholders);
        this.generator = generator;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<ItemPresetPlaceholder> placeholders() {
        return placeholders;
    }

    public boolean configurable() {
        return !placeholders.isEmpty();
    }

    public ItemStack generate() {
        return generate(Map.of());
    }

    public ItemStack generate(Map<String, String> values) {
        try {
            ItemStack stack = generator.generate(new ItemPresetContext(values == null ? Map.of() : values));
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }
}
