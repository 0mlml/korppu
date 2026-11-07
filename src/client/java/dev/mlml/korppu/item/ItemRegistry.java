package dev.mlml.korppu.item;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.mlml.korppu.KorppuMod;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.HolderLookup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a file-persisted list of item stacks.
 * Items are stored as SNBT (Stringified NBT) strings in korppu_items.json.
 */
public class ItemRegistry {
    private static final Path FILE = Path.of("korppu_items.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Entry(String name, String snbt) {}

    private static final List<Entry> entries = new ArrayList<>();

    public static List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static void add(Entry entry) {
        entries.add(entry);
        save();
    }

    public static void update(int index, Entry entry) {
        if (index < 0 || index >= entries.size()) return;
        entries.set(index, entry);
        save();
    }

    public static void remove(int index) {
        if (index < 0 || index >= entries.size()) return;
        entries.remove(index);
        save();
    }

    public static String serialize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        HolderLookup.Provider reg = getRegistries();
        if (reg == null) return null;

        DynamicOps<Tag> ops = reg.createSerializationContext(NbtOps.INSTANCE);
        DataResult<Tag> result = ItemStack.CODEC.encodeStart(ops, stack);
        return result.result()
                .filter(e -> e instanceof CompoundTag)
                .map(Object::toString)
                .orElse(null);
    }

    public static ItemStack deserialize(String snbt) {
        if (snbt == null || snbt.isBlank()) return ItemStack.EMPTY;
        HolderLookup.Provider reg = getRegistries();
        if (reg == null) return ItemStack.EMPTY;

        try {
            CompoundTag nbt = TagParser.parseCompoundFully(snbt);
            DynamicOps<Tag> ops = reg.createSerializationContext(NbtOps.INSTANCE);
            DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, nbt);
            return result.result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            KorppuMod.LOGGER.warn("ItemRegistry: failed to deserialize SNBT: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack parseComponentSyntax(String str) {
        if (str == null || str.isBlank()) return ItemStack.EMPTY;
        HolderLookup.Provider reg = getRegistries();
        if (reg == null) return ItemStack.EMPTY;

        try {
            ItemParser reader = new ItemParser(reg);
            ItemInput result = reader.parse(new StringReader(str));
            return result.createItemStack(1);
        } catch (Exception e) {
            KorppuMod.LOGGER.warn("ItemRegistry: failed to parse component syntax: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    public static void load() {
        entries.clear();
        if (!Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String name = obj.get("name").getAsString();
                String snbt = obj.get("snbt").getAsString();
                entries.add(new Entry(name, snbt));
            }
        } catch (Exception e) {
            KorppuMod.LOGGER.warn("ItemRegistry: failed to load: {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            JsonArray arr = new JsonArray();
            for (Entry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", e.name());
                obj.addProperty("snbt", e.snbt());
                arr.add(obj);
            }
            Files.writeString(FILE, GSON.toJson(arr));
        } catch (IOException e) {
            KorppuMod.LOGGER.warn("ItemRegistry: failed to save: {}", e.getMessage());
        }
    }

    private static HolderLookup.Provider getRegistries() {
        if (KorppuMod.mc.level == null) {
            KorppuMod.LOGGER.warn("ItemRegistry: world is null, cannot access registries");
            return null;
        }
        return KorppuMod.mc.level.registryAccess();
    }
}
