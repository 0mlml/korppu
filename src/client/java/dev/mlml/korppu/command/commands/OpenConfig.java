package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;

public class OpenConfig extends Command {
    public OpenConfig() {
        super("config", "Manage config for a module", "cfg");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            ConsoleWindow.print(String.format("Usage: %sconfig <module>", ModuleManager.getCommandPrefix()));
            return;
        }

        Module module = ModuleManager.getModuleByStringIgnoreCase(args[0]);

        if (module == null) {
            ConsoleWindow.print("Module not found.");
            return;
        }

        KorppuImGui.openModuleConfig(module);
    }
}
