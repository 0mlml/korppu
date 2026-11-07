package dev.mlml.korppu.command.arg;

import dev.mlml.korppu.command.exception.CommandException;

public class IntArgParser implements ArgParser<Integer> {
    @Override
    public Integer parse(String argument) throws CommandException {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid integer value: " + argument);
        }
    }
}
