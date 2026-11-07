package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class Heads extends Command {
    public Heads() {
        super("heads", "Gives a player head by name via command", "head");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        if (index == 0) {
            if (KorppuMod.mc.level == null) return new PossibleArg(ArgType.PLAYER);
            return new PossibleArg(ArgType.PLAYER,
                    KorppuMod.mc.level.players().stream()
                            .map(p -> p.getGameProfile().name())
                            .toArray(String[]::new));
        }
        return super.getSuggestionsWithType(index, args);
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");
        if (!KorppuMod.mc.player.getAbilities().instabuild) throw new CommandException("Creative mode required.");
        validateArgsLength(args, 1, "Usage: heads <playerName>");

        String name = args[0].trim();
        if (name.isEmpty() || name.length() > 16) throw new CommandException("Invalid player name.");

        String command = "give @s minecraft:player_head[minecraft:profile={name:\"" + name + "\"}]";
        KorppuMod.mc.player.connection.sendCommand(command);
        ConsoleWindow.print("Executed /" + command);
    }
}
