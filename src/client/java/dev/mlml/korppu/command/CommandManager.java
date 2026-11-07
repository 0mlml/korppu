package dev.mlml.korppu.command;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.commands.*;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    @Getter
    private static final List<Command> commands = new ArrayList<>();

    public static void init() {
        commands.add(new SaveConfig());
        commands.add(new ReadConfig());
        commands.add(new ConfigSelfTest());
        commands.add(new OpenConfig());
        commands.add(new Say());
        commands.add(new VClip());
        commands.add(new HClip());
        commands.add(new Help());
        commands.add(new Invsee());
        commands.add(new Panic());
        commands.add(new Reconnect());
        commands.add(new Center());
        commands.add(new ClearChat());
        commands.add(new Seed());
        commands.add(new SetVelocity());
        commands.add(new Server());
        commands.add(new Give());
        commands.add(new Heads());
        commands.add(new SaveSkin());
        commands.add(new SetBlock());
        commands.add(new Locate());
        commands.add(new Crash());
        commands.add(new ForceOpBook());
        commands.add(new ForceOpSign());
        commands.add(new ForceOPServerKill());
        commands.add(new BlockListManager());
        commands.add(new TP());
        // New commands from integration
        commands.add(new ViewNbt());
        commands.add(new WorldInfo());
        commands.add(new GarbageCleaner());

        KorppuMod.eventManager.register(CommandManager.class);
        KorppuMod.LOGGER.info("Initialized " + commands.size() + " commands");
    }

    public static Command getCommand(Class<? extends Command> commandClass) {
        for (Command command : commands) {
            if (command.getClass().equals(commandClass)) {
                return command;
            }
        }

        return null;
    }

    public static Command getCommandByAlias(String alias) {
        for (Command command : commands) {
            for (String commandAlias : command.getAliases()) {
                if (commandAlias.equalsIgnoreCase(alias)) {
                    return command;
                }
            }
        }

        return null;
    }

    public static void execute(String command) {
        if (command.isBlank()) {
            return;
        }
        String[] spl = command.split(" +");
        if (spl.length == 0) {
            return;
        }
        String cmd = spl[0].toLowerCase();
        String[] args = Arrays.copyOfRange(spl, 1, spl.length);
        Command c = getCommandByAlias(cmd);
        if (c == null) {
            ConsoleWindow.print("Unknown command: " + cmd);
        } else {
            try {
                c.execute(args);
            } catch (CommandException cex) {
                ConsoleWindow.print("[Error] " + cex.getMessage());
            } catch (Exception e) {
                ConsoleWindow.print("[Error] " + e.getMessage());
                KorppuMod.LOGGER.error("Error running command: " + command, e);
            }
        }
    }
}
