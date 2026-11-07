package dev.mlml.korppu.command;

import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.exception.CommandException;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Stream;

public abstract class Command {
    @Getter
    private final String name;
    @Getter
    private final String description;
    private final String[] aliases;

    public Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    public String[] getAliases() {
        return Stream.concat(Stream.of(name), Arrays.stream(aliases).filter(alias -> alias != null && !alias.isBlank()))
                .toArray(String[]::new);
    }

    public abstract void execute(String[] args) throws CommandException;

    public ExamplesEntry getExampleArgs() {
        return null;
    }

    protected void validateArgsLength(String[] args, int requiredLength, String message) throws CommandException {
        if (args.length < requiredLength) {
            throw new CommandException(message == null ? "Invalid number of arguments: " + requiredLength + " arguments required" : message);
        }
    }

    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return new PossibleArg(null);
    }

    public record ExamplesEntry(String... examples) {
    }
}
