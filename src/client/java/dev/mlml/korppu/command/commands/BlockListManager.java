package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.BlockESP;

import java.util.Set;

public class BlockListManager extends Command {
    public BlockListManager() {
        super("blocklist", "Manages BlockESP blocklist", "bl");
    }

    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return switch (index) {
            case 0 -> new PossibleArg(ArgType.STRING, "add", "remove", "list", "clear");
            case 1 -> switch (args[0].toLowerCase()) {
                case "add" -> new PossibleArg(ArgType.STRING, "<Block name>");
                case "list", "clear" -> new PossibleArg(ArgType.STRING, "No additional arguments needed");
                case "remove" -> new PossibleArg(ArgType.STRING, getBlockListArray());
                default -> new PossibleArg(ArgType.STRING, "Invalid action");
            };
            default -> super.getSuggestionsWithType(index, args);
        };
    }

    @Override
    public void execute(String[] args) throws CommandException {
        validateArgsLength(args, 1, "Usage: blocklist <add|remove|list|clear> [<Block name>]");
        String action = args[0].toLowerCase();
        switch (action) {
            case "add" -> {
                requireArg(args, 1, "Block name required");
                addBlock(args[1]);
            }
            case "remove" -> {
                requireArg(args, 1, "Block name required");
                removeBlock(args[1]);
            }
            case "list" -> listBlocks();
            case "clear" -> clearBlockList();
            default -> throw new CommandException("Invalid action: " + action + ". Use add, remove, list, or clear.");
        }
    }

    private void requireArg(String[] args, int index, String message) throws CommandException {
        if (args.length <= index || args[index].isEmpty()) throw new CommandException(message);
    }

    private BlockESP getModule() throws CommandException {
        BlockESP besp = ModuleManager.getModule(BlockESP.class);
        if (besp == null) throw new CommandException("BlockESP module not found.");
        return besp;
    }

    private String[] getBlockListArray() {
        BlockESP besp = ModuleManager.getModule(BlockESP.class);
        if (besp == null) return new String[0];
        return besp.getBlockList().getValue().toArray(new String[0]);
    }

    private void addBlock(String name) throws CommandException {
        BlockESP besp = getModule();
        if (besp.getBlockList().isSelected(name)) {
            ConsoleWindow.print("Block already in list: " + name);
            return;
        }
        besp.getBlockList().toggle(name);
        ConsoleWindow.print("Added block: " + name);
    }

    private void removeBlock(String name) throws CommandException {
        BlockESP besp = getModule();
        if (!besp.getBlockList().isSelected(name)) {
            ConsoleWindow.print("Block not in list: " + name);
            return;
        }
        besp.getBlockList().toggle(name);
        ConsoleWindow.print("Removed block: " + name);
    }

    private void listBlocks() throws CommandException {
        Set<String> blocks = getModule().getBlockList().getValue();
        if (blocks.isEmpty()) {
            ConsoleWindow.print("Block list is empty.");
            return;
        }
        ConsoleWindow.print("Block list: " + String.join(", ", blocks));
    }

    private void clearBlockList() throws CommandException {
        BlockESP besp = getModule();
        besp.getBlockList().getValue().clear();
        ConsoleWindow.print("Cleared block list.");
    }
}
