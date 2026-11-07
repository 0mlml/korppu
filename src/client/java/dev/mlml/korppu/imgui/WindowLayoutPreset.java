package dev.mlml.korppu.imgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.mlml.korppu.KorppuMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowLayoutPreset {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("korppu_presets.json");

    public String name;
    public boolean moduleList;
    public boolean console;
    public boolean entityList;
    public boolean textEditor;
    public boolean itemRegistry;
    public List<String> statusModules = new ArrayList<>();
    public List<String> configModules = new ArrayList<>();
    /** Module name → was enabled. Null on old presets that pre-date this field. */
    public Map<String, Boolean> enabledModules = new HashMap<>();
    /** Module name → serialized settings string (Config.serialize() format). */
    public Map<String, String> moduleConfigs = new HashMap<>();

    public WindowLayoutPreset(String name) {
        this.name = name;
    }

    public static List<WindowLayoutPreset> load() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try (Reader r = Files.newBufferedReader(FILE)) {
            List<WindowLayoutPreset> list = GSON.fromJson(r, new TypeToken<List<WindowLayoutPreset>>() {}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            KorppuMod.LOGGER.error("Failed to load presets", e);
            return new ArrayList<>();
        }
    }

    public static void save(List<WindowLayoutPreset> presets) {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(presets, w);
        } catch (Exception e) {
            KorppuMod.LOGGER.error("Failed to save presets", e);
        }
    }
}
