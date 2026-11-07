package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class ForceOpSign extends Command {
    public ForceOpSign() {
        super("forceopsign", "Places a command-click sign payload near you", "opsign");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0 -> new PossibleArg(ArgType.STRING, "targetName");
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");
        validateArgsLength(args, 1, "Usage: forceopsign <targetName>");

        String target = args[0];
        int x = KorppuMod.mc.player.getBlockX();
        int y = KorppuMod.mc.player.getBlockY();
        int z = KorppuMod.mc.player.getBlockZ();

        String cmd = "setblock " + x + " " + y + " " + z + " minecraft:oak_sign[rotation=0]{front_text:{messages:['{\"text\":\"[OP]\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/op " + target + "\"}}','{\"text\":\"Click me\"}','{\"text\":\"\"}','{\"text\":\"\"}']}}";
        KorppuMod.mc.player.connection.sendCommand(cmd);
        ConsoleWindow.print("Sent force-op sign payload for " + target + ".");
    }
}
