package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.client.multiplayer.ServerData;

public class Server extends Command {
    public Server() {
        super("server", "Print current server info", "serverinfo");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        ServerData info = KorppuMod.mc.getCurrentServer();
        if (info == null) {
            ConsoleWindow.print("Not connected to a multiplayer server.");
            return;
        }

        ConsoleWindow.print("=== Server Info ===");
        ConsoleWindow.print("Name: " + info.name);
        ConsoleWindow.print("Address: " + info.ip);
        if (KorppuMod.mc.getConnection() != null) {
            ConsoleWindow.print("Players (tab): " + KorppuMod.mc.getConnection().getListedOnlinePlayers().size());
        }
        if (KorppuMod.mc.level != null) {
            ConsoleWindow.print("Dimension: " + KorppuMod.mc.level.dimension().identifier());
        }
    }
}
