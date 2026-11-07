package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class ReadConfig extends Command {
    public ReadConfig() {
        super("readconfig", "Read config from a file", "read");
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            ConfigWriter.readConfigFromFile(args[0]);
            ConsoleWindow.print(String.format("Config read from %s.", args[0]));
        } else {
            ConfigWriter.readConfigFromFile();
            ConsoleWindow.print("Config read.");
        }

    }
}
