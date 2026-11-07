package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.arg.DoubleArgParser;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.coloring.StaticArgServer;
import dev.mlml.korppu.command.exception.CommandException;
import net.minecraft.client.player.LocalPlayer;

public class VClip extends Command {
    public VClip() {
        super("vclip", "Clip vertically", "vc");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return StaticArgServer.serveFromStatic(index, new PossibleArg(ArgType.NUMBER, "<amount>"));
    }

    @Override
    public void execute(String[] args) throws CommandException {
        validateArgsLength(args, 1, "Provide height");

        LocalPlayer player = KorppuMod.mc.player;
        player.setPos(player.getX(), player.getY() + new DoubleArgParser().parse(args[0]), player.getZ());
    }
}
