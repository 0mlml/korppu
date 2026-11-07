package dev.mlml.korppu.command.arg;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.exception.CommandException;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerFromUUIDArgParser implements ArgParser<Player> {
    @Override
    public Player parse(String argument) throws CommandException {
        if (KorppuMod.mc.level == null) {
            throw new CommandException("World is not loaded");
        }
        try {
            UUID u = UUID.fromString(argument);
            for (AbstractClientPlayer player : KorppuMod.mc.level.players()) {
                if (player.getUUID().equals(u)) {
                    return player;
                }
            }
            throw new CommandException("Player not found with UUID: " + argument);
        } catch (Exception e) {
            throw new CommandException("Invalid UUID format: " + argument);
        }
    }
}
