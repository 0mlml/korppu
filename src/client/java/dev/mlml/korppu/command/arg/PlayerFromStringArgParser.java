package dev.mlml.korppu.command.arg;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.exception.CommandException;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerFromStringArgParser implements ArgParser<Player> {
    final boolean ignoreCase;

    public PlayerFromStringArgParser(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Player parse(String argument) throws CommandException {
        if (KorppuMod.mc.level == null) {
            throw new CommandException("World is not loaded");
        }
        for (AbstractClientPlayer player : KorppuMod.mc.level.players()) {
            if (ignoreCase) {
                if (player.getGameProfile().name().equalsIgnoreCase(argument)) {
                    return player;
                }
            } else {
                if (player.getGameProfile().name().equals(argument)) {
                    return player;
                }
            }
        }
        throw new CommandException("Player not found: " + argument);
    }
}
