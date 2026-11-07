package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigSelfTest extends Command {
    public ConfigSelfTest() {
        super("configselftest", "Round-trip test config save/load stability", "cfgtest", "selftestcfg");
    }

    @Override
    public void execute(String[] args) {
        Map<String, String> before = snapshotModules();
        String serialized = ConfigWriter.generateConfig();

        ConfigWriter.deserializeConfig(serialized);

        Map<String, String> after = snapshotModules();
        String serializedAfter = ConfigWriter.generateConfig();

        int mismatches = 0;
        StringBuilder firstDiffs = new StringBuilder();
        for (Map.Entry<String, String> entry : before.entrySet()) {
            String name = entry.getKey();
            String a = entry.getValue();
            String b = after.get(name);
            if (b == null || !a.equals(b)) {
                mismatches++;
                if (mismatches <= 8) {
                    if (!firstDiffs.isEmpty()) firstDiffs.append(", ");
                    firstDiffs.append(name);
                }
            }
        }

        boolean bytePerfect = serialized.equals(serializedAfter);
        if (mismatches == 0 && bytePerfect) {
            ConsoleWindow.print("Config self-test passed: round-trip is stable.");
            return;
        }

        ConsoleWindow.print("Config self-test failed: " + mismatches + " module mismatches"
                + (bytePerfect ? "" : ", serialized output changed") + ".");
        if (!firstDiffs.isEmpty()) {
            ConsoleWindow.print("First mismatches: " + firstDiffs);
        }
    }

    private static Map<String, String> snapshotModules() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Module module : ModuleManager.getModules()) {
            out.put(module.getName(), module.isEnabled() + "|" + module.getConfig().serialize());
        }
        return out;
    }
}
