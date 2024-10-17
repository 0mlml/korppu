package dev.mlml.korppu.command;

import lombok.Getter;

public abstract class Command {
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final String[] aliases;

    public Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = new String[aliases.length + 1];
        this.aliases[0] = name;
        System.arraycopy(aliases, 0, this.aliases, 1, aliases.length);
    }

    public abstract void execute(String[] args);
}
