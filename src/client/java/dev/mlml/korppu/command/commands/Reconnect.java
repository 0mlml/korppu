package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public class Reconnect extends Command {
    public Reconnect() {
        super("reconnect", "Disconnects and reconnects to the current server", "rejoin");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        ServerData current = KorppuMod.mc.getCurrentServer();
        if (current == null) {
            ConsoleWindow.print("Not connected to a multiplayer server.");
            return;
        }

        if (KorppuMod.mc.level != null) {
            KorppuMod.mc.level.disconnect(Component.literal("Reconnecting"));
        }

        ConnectScreen.startConnecting(
                new JoinMultiplayerScreen(new TitleScreen()),
                KorppuMod.mc,
                ServerAddress.parseString(current.ip),
                current,
                false,
                null
        );
    }
}
