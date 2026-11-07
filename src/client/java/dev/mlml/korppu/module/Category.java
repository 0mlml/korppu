package dev.mlml.korppu.module;

import lombok.Getter;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    CLIENT("Client"),
    EXPLOIT("Exploit"),
    UNSORTED("Unsorted");

    @Getter
    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
}
