package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class SaveConfig extends Command {
    public SaveConfig() {
        super("saveconfig", "Save config to file", "save");
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            ConfigWriter.writeConfigToFile(args[0]);
            ConsoleWindow.print(String.format("Config saved to %s.", args[0]));
        } else {
            ConfigWriter.writeConfigToFile();
            ConsoleWindow.print("Config saved.");
        }

    }
}
