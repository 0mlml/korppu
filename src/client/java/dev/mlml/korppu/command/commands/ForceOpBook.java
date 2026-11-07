package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class ForceOpBook extends Command {
    public ForceOpBook() {
        super("forceopbook", "Gives a clickable OP book payload via /give command", "opbook");
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
        validateArgsLength(args, 1, "Usage: forceopbook <targetName>");

        String target = args[0];
        String payload = "give @s minecraft:written_book[minecraft:written_book_content={title:\"Operator\",author:\"Korppu\",pages:[\"{\\\"text\\\":\\\"Grant OP\\\",\\\"clickEvent\\\":{\\\"action\\\":\\\"run_command\\\",\\\"value\\\":\\\"/op " + target + "\\\"}}\"]}]";
        KorppuMod.mc.player.connection.sendCommand(payload);
        ConsoleWindow.print("Sent force-op book payload for " + target + ".");
    }
}
