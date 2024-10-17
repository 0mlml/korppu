package dev.mlml.korppu.command;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.commands.OpenConfig;
import dev.mlml.korppu.command.commands.ReadConfig;
import dev.mlml.korppu.command.commands.SaveConfig;
import dev.mlml.korppu.command.commands.Say;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.ChatSendEvent;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.module.ModuleManager;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandManager {
    @Getter
    private static final List<Command> commands = new ArrayList<>();

    public static void init() {
        commands.add(new SaveConfig());
        commands.add(new ReadConfig());
        commands.add(new OpenConfig());
        commands.add(new Say());

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

    public static void execute(String commandString) {
        if (commandString.isEmpty() || commandString.isBlank()) {
            return;
        }

        String[] args = commandString.split(" +");

        String commandName = args[0];
        String[] commandArgs = new String[args.length - 1];

        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        Command command = getCommandByAlias(commandName);

        if (Objects.isNull(command)) {
            ChatHelper.message(TextFormatter.format("%1Error:%4 Command \"%3%s%4\" not found. Use %2say %3%s%4 to send it in chat.", TextFormatter.Code.RED, TextFormatter.Code.GREEN, TextFormatter.Code.YELLOW, TextFormatter.Code.RESET, commandName, ModuleManager.getCommandPrefix(), commandName));
            return;
        }

        command.execute(commandArgs);
    }

    @Listener
    public static void onMessage(ChatSendEvent event) {
        String message = event.getMessage();

        KorppuMod.LOGGER.debug("Received chat message: " + message);

        if (message.startsWith(ModuleManager.getCommandPrefix())) {
            event.cancel();
            execute(message.substring(ModuleManager.getCommandPrefix().length()));
        }
    }
}
