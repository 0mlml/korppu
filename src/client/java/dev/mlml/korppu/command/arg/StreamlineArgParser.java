package dev.mlml.korppu.command.arg;

import dev.mlml.korppu.command.exception.CommandException;
import net.minecraft.world.entity.player.Player;

public class StreamlineArgParser {
    final String[] args;
    int index = 0;

    public StreamlineArgParser(String[] args) {
        this.args = args;
    }

    public String consumeString() throws CommandException {
        return consumeString(null);
    }

    public String consumeString(String defaultValue) throws CommandException {
        if (index >= args.length) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new CommandException("Not enough arguments");
        }
        return args[index++];
    }

    public boolean isEmpty() {
        return index >= args.length;
    }

    public int consumeInt(Integer defaultValue) throws CommandException {
        return new IntArgParser().parse(consumeString(defaultValue == null ? null : defaultValue.toString()));
    }

    public int consumeInt() throws CommandException {
        return consumeInt(null);
    }

    public double consumeDouble() throws CommandException {
        return new DoubleArgParser().parse(consumeString());
    }

    public Player consumePlayerEntityFromName(boolean ignoreCase) throws CommandException {
        return new PlayerFromStringArgParser(ignoreCase).parse(consumeString());
    }

    public Player consumePlayerEntityFromUuid() throws CommandException {
        return new PlayerFromUUIDArgParser().parse(consumeString());
    }
}
