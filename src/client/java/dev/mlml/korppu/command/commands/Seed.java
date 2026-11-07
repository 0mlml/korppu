package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.misc.SeedState;

public class Seed extends Command {
    public Seed() {
        super("seed", "Get or set local world seed for seed-based modules", "worldseed");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0 -> new PossibleArg(ArgType.STRING, "get", "set", "clear");
            case 1 -> "set".equalsIgnoreCase(args[0])
                    ? new PossibleArg(ArgType.NUMBER, "seed")
                    : super.getSuggestionsWithType(index, args);
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length == 0 || "get".equalsIgnoreCase(args[0])) {
            Long seed = SeedState.getSeed();
            ConsoleWindow.print(seed == null ? "Seed is not set." : "Seed: " + seed);
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "set" -> {
                validateArgsLength(args, 2, "Usage: seed set <number>");
                long seed;
                try {
                    seed = Long.parseLong(args[1]);
                } catch (NumberFormatException ex) {
                    throw new CommandException("Invalid seed: " + args[1]);
                }
                SeedState.setSeed(seed, "command");
                ConsoleWindow.print("Seed set to " + seed);
            }
            case "clear" -> {
                SeedState.setSeed(null, "command");
                ConsoleWindow.print("Seed cleared.");
            }
            default -> throw new CommandException("Usage: seed [get|set <number>|clear]");
        }
    }
}
