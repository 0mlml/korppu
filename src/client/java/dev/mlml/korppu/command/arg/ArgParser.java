package dev.mlml.korppu.command.arg;

import dev.mlml.korppu.command.exception.CommandException;

public interface ArgParser<T> {
    T parse(String argument) throws CommandException;
}