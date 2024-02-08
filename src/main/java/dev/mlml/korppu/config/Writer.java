package dev.mlml.korppu.config;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Writer {
    public static final String SchemaVersion = "1";
    public static final String configFile = "korppu.txt";
    public static final String SEPARATOR = "\u001F";

    public static String generateConfig() {
        StringBuilder config = new StringBuilder(String.format("Korppu %s\n", SchemaVersion));

        for (Module module : ModuleManager.getModules()) {
            config.append("m")
                  .append(SEPARATOR)
                  .append(module.getName())
                  .append(SEPARATOR)
                  .append(module.isEnabled())
                  .append("\n");
            config.append(module.getConfig().serialize()).append("\n");
        }

        return config.toString();
    }

    public static void deserializeConfig(String config) {
        String[] lines = config.split("\n");

        Module currentModule = null;
        List<String> moduleLines = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(SEPARATOR);

            if (parts.length < 3) {
                continue;
            }

            if (parts[0].equals("m")) {
                if (currentModule != null) {
                    currentModule.getConfig().deserialize(moduleLines);
                }

                Module module = ModuleManager.getModuleByString(parts[1]);

                if (module == null) {
                    continue;
                }

                module.setEnabled(Boolean.parseBoolean(parts[2]));

                currentModule = module;
                moduleLines.clear();
            } else {
                moduleLines.add(line);
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
        File file = new File(filename);

        if (!file.exists()) {
            KorppuMod.LOGGER.warn("Config file does not exist");
            return;
        }

        try {
            java.io.FileReader fr = new java.io.FileReader(file);
            StringBuilder config = new StringBuilder();
            int c;
            while ((c = fr.read()) != -1) {
                config.append((char) c);
            }
            fr.close();
            deserializeConfig(config.toString());
        } catch (Exception e) {
            KorppuMod.LOGGER.error("Failed to read config from file:", e);
        }
    }

    public static void writeConfigToFile() {
        writeConfigToFile(configFile);
    }

    public static void writeConfigToFile(String filename) {
        String config = generateConfig();

        File file = new File(filename);

        try {
            java.io.FileWriter fw = new java.io.FileWriter(file);
            fw.write(config);
            fw.close();
        } catch (Exception e) {
            KorppuMod.LOGGER.error("Failed to write config to file: {}", e.getMessage());
        }
    }
}
