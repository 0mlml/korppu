package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class SetVelocity extends Command {
    public SetVelocity() {
        super("setvelocity", "Sets local player velocity vector", "vel");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0, 1, 2 -> new PossibleArg(ArgType.NUMBER, "amount");
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");
        validateArgsLength(args, 3, "Usage: setvelocity <x> <y> <z>");

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[0]);
            y = Double.parseDouble(args[1]);
            z = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            throw new CommandException("Velocity values must be numbers.");
        }

        KorppuMod.mc.player.setDeltaMovement(x, y, z);
        ConsoleWindow.print(String.format("Velocity set: %.3f %.3f %.3f", x, y, z));
    }
}
