package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.arg.DoubleArgParser;
import dev.mlml.korppu.command.arg.PlayerFromStringArgParser;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.world.entity.player.Player;


public class TP extends Command {
    public TP() {
        super("tp", "Insta TP (will not work on most servers)", "teleport");
    }

    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0 -> new PossibleArg(ArgType.STRING, "player", "pos");
            case 1 -> switch (args[0].toLowerCase()) {
                case "player" ->
                        KorppuMod.mc.level == null ? new PossibleArg(ArgType.PLAYER) :
                        new PossibleArg(ArgType.PLAYER,
                                KorppuMod.mc.level.players().stream().map(abstractClientPlayerEntity -> abstractClientPlayerEntity.getGameProfile().name()).toList().toArray(String[]::new));
                case "pos" -> new PossibleArg(ArgType.NUMBER, "amount");
                default -> new PossibleArg(ArgType.STRING, "Invalid action");
            };
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        validateArgsLength(args, 1, "Usage: tp <player|pos> [<target>]");
        String action = args[0].toLowerCase();

        ConsoleWindow.print(String.format("Previous position: X: %.2f, Y: %.2f, Z: %.2f", KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY(), KorppuMod.mc.player.getZ()));

        switch (action) {
            case "player" -> {
                validateArgsLength(args, 2, "Usage: tp player <target>");
                String target = args[1];
                Player player = new PlayerFromStringArgParser(true).parse(target);
                if (player == null) {
                    ConsoleWindow.print("Player not found: " + target);
                    return;
                }
                KorppuMod.mc.player.setPos(player.getX(), player.getY(), player.getZ());
                ConsoleWindow.print("Packet sent.");
            }
            case "pos" -> {
                validateArgsLength(args, 4, "Usage: tp pos <x> <y> <z>");
                double x = new DoubleArgParser().parse(args[1]);
                double y = new DoubleArgParser().parse(args[2]);
                double z = new DoubleArgParser().parse(args[3]);
                KorppuMod.mc.player.setPos(x, y, z);
                ConsoleWindow.print("Packet sent to position: X: " + x + ", Y: " + y + ", Z: " + z);
            }
            default -> ConsoleWindow.print("Invalid action: " + action);
        }
    }
}
