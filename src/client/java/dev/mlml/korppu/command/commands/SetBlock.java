package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class SetBlock extends Command {
    public SetBlock() {
        super("setblock", "Runs setblock using your current permissions", "sb");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0, 1, 2 -> new PossibleArg(ArgType.NUMBER, "coord");
            case 3 -> new PossibleArg(ArgType.STRING,
                    BuiltInRegistries.BLOCK.stream().map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).toArray(String[]::new));
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");
        if (!KorppuMod.mc.player.getAbilities().instabuild) throw new CommandException("Creative mode required.");
        validateArgsLength(args, 4, "Usage: setblock <x> <y> <z> <block_id>");

        int x;
        int y;
        int z;
        try {
            x = (int) Math.floor(Double.parseDouble(args[0]));
            y = (int) Math.floor(Double.parseDouble(args[1]));
            z = (int) Math.floor(Double.parseDouble(args[2]));
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid coordinates.");
        }

        Identifier blockId;
        try {
            blockId = Identifier.parse(args[3]);
        } catch (Exception e) {
            throw new CommandException("Invalid block id: " + args[3]);
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null) throw new CommandException("Unknown block: " + args[3]);

        String cmd = "setblock " + x + " " + y + " " + z + " " + blockId;
        KorppuMod.mc.player.connection.sendCommand(cmd);
        ConsoleWindow.print("Executed /" + cmd);
    }
}
