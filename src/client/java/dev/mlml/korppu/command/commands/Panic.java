package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;

public class Panic extends Command {
    public Panic() {
        super("panic", "Disables all currently enabled modules");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        int disabled = 0;

        for (Module module : ModuleManager.getModules()) {
            if (module.isEnabled()) {
                module.setEnabled(false);
                disabled++;
            }
        }

        ConsoleWindow.print("Panic complete: disabled " + disabled + " modules.");
    }
}
