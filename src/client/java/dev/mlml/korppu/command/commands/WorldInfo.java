package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.Difficulty;

public class WorldInfo extends Command {
    public WorldInfo() {
        super("worldinfo", "Prints information about the current world/server", "winfo");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            throw new CommandException("Not in a world");
        }

        ClientLevel world = KorppuMod.mc.level;
        var player = KorppuMod.mc.player;

        ConsoleWindow.print("=== World Info ===");
        ConsoleWindow.print("Dimension: " + world.dimension().identifier());
        ConsoleWindow.print("Difficulty: " + world.getDifficulty().getDisplayName().getString());
        ConsoleWindow.print("Time: " + world.getOverworldClockTime() + " (day " + (world.getOverworldClockTime() / 24000) + ")");
        ConsoleWindow.print("Weather: " + (world.isThundering() ? "Thunder" : world.isRaining() ? "Rain" : "Clear"));
        // Note: spawn position may not be accessible from client
        // ConsoleWindow.print("Spawn: " + world.getSpawnPos().toShortString());

        // Player info
        ConsoleWindow.print("=== Player Info ===");
        ConsoleWindow.print("Position: " + String.format("%.2f, %.2f, %.2f", player.getX(), player.getY(), player.getZ()));
        ConsoleWindow.print("Block: " + player.blockPosition().toShortString());
        ConsoleWindow.print("Chunk: " + (player.getBlockX() >> 4) + ", " + (player.getBlockZ() >> 4));
        ConsoleWindow.print("Gamemode: " + (player.getAbilities().instabuild ? "Creative" :
                player.getAbilities().mayfly ? "Spectator/Admin" : "Survival/Adventure"));
        ConsoleWindow.print("Health: " + String.format("%.1f / %.1f", player.getHealth(), player.getMaxHealth()));
        ConsoleWindow.print("Food: " + player.getFoodData().getFoodLevel());
    }
}
