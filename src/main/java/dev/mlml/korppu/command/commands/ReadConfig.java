package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.config.Writer;
import dev.mlml.korppu.gui.ChatHelper;

public class ReadConfig extends Command {
    public ReadConfig() {
        super("readconfig", "Read config from a file", "read");
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            Writer.readConfigFromFile(args[0]);
            ChatHelper.message(String.format("Config read from %s.", args[0]));
        } else {
            Writer.readConfigFromFile();
            ChatHelper.message("Config read.");
        }

    }
}
