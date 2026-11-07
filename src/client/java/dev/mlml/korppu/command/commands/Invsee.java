package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.arg.PlayerFromStringArgParser;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.coloring.StaticArgServer;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.misc.Utils;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;


public class Invsee extends Command {
    public Invsee() {
        super("invsee", "See the inventory of a player", "inv");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        if (KorppuMod.mc.level == null) return StaticArgServer.serveFromStatic(index, new PossibleArg(ArgType.STRING));
        return StaticArgServer.serveFromStatic(index, new PossibleArg(ArgType.STRING,
                KorppuMod.mc.level.players().stream().map(abstractClientPlayerEntity -> abstractClientPlayerEntity.getGameProfile().name()).toList().toArray(String[]::new)));
    }

    @Override
    public void execute(String[] args) throws CommandException {
        validateArgsLength(args, 1, "Provide target username");
        Player t = new PlayerFromStringArgParser(true).parse(args[0]);
        Utils.TickManager.runOnNextRender(() -> KorppuMod.mc.setScreen(new InventoryScreen(t)));
        ConsoleWindow.print("Opened inventory of " + t.getGameProfile().name() + ".");
    }
}
