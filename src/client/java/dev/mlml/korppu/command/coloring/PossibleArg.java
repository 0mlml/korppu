package dev.mlml.korppu.command.coloring;

import java.util.function.Supplier;

public record PossibleArg(ArgType argType, Supplier<String[]> suggestionSupplier) {
    public PossibleArg(ArgType type, String... suggestions) {
        this(type, () -> suggestions);
    }
}