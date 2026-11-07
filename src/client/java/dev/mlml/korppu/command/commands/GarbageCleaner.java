package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

public class GarbageCleaner extends Command {
    public GarbageCleaner() {
        super("gc", "Runs the Java garbage collector to free memory", "garbagecollect");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.gc();
        long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long freed = before - after;
        ConsoleWindow.print("GC complete. Used: " + formatBytes(after) + " (freed " + formatBytes(freed) + ")");
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) bytes = 0;
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
