package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.config.Writer;
import dev.mlml.korppu.gui.ChatHelper;

public class SaveConfig extends Command {
    public SaveConfig() {
        super("saveconfig", "Save config to file", "save");
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            Writer.writeConfigToFile(args[0]);
            ChatHelper.message(String.format("Config saved to %s.", args[0]));
        } else {
            Writer.writeConfigToFile();
            ChatHelper.message("Config saved.");
        }

    }
}
