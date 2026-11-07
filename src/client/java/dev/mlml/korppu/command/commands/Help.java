package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.CommandManager;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class Help extends Command {
    public Help() {
        super("help", "Get help", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        StringBuilder message = new StringBuilder();

        message.append("Commands: ");
        for (Command command : CommandManager.getCommands()) {
            message.append(command.getName()).append(", ");
        }

        ConsoleWindow.print(message.toString());
    }
}
