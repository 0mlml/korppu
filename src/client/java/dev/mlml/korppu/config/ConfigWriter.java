package dev.mlml.korppu.config;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigWriter {
    public static final String SchemaVersion = "1";
    public static final String configFile = "korppu.txt";
    public static final String GROUP_SEPARATOR = "\u001D";
    public static final String RECORD_SEPARATOR = "\u001E";
    public static final String UNIT_SEPARATOR = "\u001F";

    public static String generateConfig() {
        StringBuilder config = new StringBuilder(String.format("Korppu %s\n", SchemaVersion));

        for (Module module : ModuleManager.getModules()) {
            config.append("m")
                    .append(RECORD_SEPARATOR)
                    .append(module.getName())
                    .append(RECORD_SEPARATOR)
                    .append(module.isEnabled())
                    .append(GROUP_SEPARATOR);
            config.append(module.getConfig().serialize()).append(GROUP_SEPARATOR);
        }

        return config.toString();
    }

    private static String[] payloadSegments(String config) {
        int newline = config.indexOf('\n');
        if (newline < 0 || newline == config.length() - 1) {
            KorppuMod.LOGGER.warn("Invalid config format: missing payload.");
            return null;
        }

        String header = config.substring(0, newline).trim();
        if (!header.startsWith("Korppu ")) {
            KorppuMod.LOGGER.warn("Invalid config header: {}", header);
            return null;
        }

        return config.substring(newline + 1).split(GROUP_SEPARATOR, -1);
    }

    /**
     * Flattens a serialized config into "Module" -> enabled and "Module/Setting" -> value entries, in file order.
     */
    public static Map<String, String> flatten(String config) {
        Map<String, String> entries = new LinkedHashMap<>();
        String[] segments = payloadSegments(config);
        if (segments == null) return entries;

        String currentModule = null;
        for (String segment : segments) {
            String[] parts = segment.split(RECORD_SEPARATOR, 3);
            if (parts.length < 3) continue;

            if (parts[0].equals("m")) {
                currentModule = parts[1];
                entries.put(currentModule, parts[2]);
            } else if (currentModule != null) {
                entries.put(currentModule + "/" + parts[1], parts[2].replace(UNIT_SEPARATOR, ", "));
            }
        }
        return entries;
    }

    public static void deserializeConfig(String config) {
        String[] segments = payloadSegments(config);
        if (segments == null) return;

        Module currentModule = null;
        List<String> moduleLines = new ArrayList<>();

        for (String segment : segments) {
            String[] parts = segment.split(RECORD_SEPARATOR, -1);

            if (parts.length < 3) {
                continue;
            }

            if (parts[0].equals("m")) {
                if (currentModule != null) {
                    currentModule.getConfig().deserialize(moduleLines);
                }

                Module module = ModuleManager.getModuleByString(parts[1]);

                KorppuMod.LOGGER.info("Deserializing module: {}", parts[1]);

                if (module == null) {
                    continue;
                }

                module.setEnabled(Boolean.parseBoolean(parts[2]));

                currentModule = module;
                moduleLines.clear();
            } else {
                moduleLines.add(segment);
            }
        }

        if (!moduleLines.isEmpty() && currentModule != null) {
            currentModule.getConfig().deserialize(moduleLines);
        }
    }

    public static void readConfigFromFile() {
        readConfigFromFile(configFile);
    }

    public static void readConfigFromFile(String filename) {
        Path file = Path.of(filename);

        if (!Files.exists(file)) {
            KorppuMod.LOGGER.warn("Config file does not exist");
            return;
        }

        try {
            deserializeConfig(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            KorppuMod.LOGGER.error("Failed to read config from file:", e);
        }
    }

    public static void writeConfigToFile() {
        writeConfigToFile(configFile);
    }

    public static void writeConfigToFile(String filename) {
        try {
            writeAtomically(Path.of(filename), generateConfig());
        } catch (Exception e) {
            KorppuMod.LOGGER.error("Failed to write config to file:", e);
        }
    }

    public static void writeAtomically(Path file, String content) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
