package dev.mlml.korppu.command.arg;

import dev.mlml.korppu.command.exception.CommandException;

public class DoubleArgParser implements ArgParser<Double> {
    @Override
    public Double parse(String argument) throws CommandException {
        try {
            return Double.parseDouble(argument);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid double value: " + argument);
        }
    }
}
