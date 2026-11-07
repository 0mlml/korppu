package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;

public class ClearChat extends Command {
    public ClearChat() {
        super("clear-chat", "Clears the chat history", "clear", "cls");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.gui == null) return;
        KorppuMod.mc.gui.getChat().clearMessages(false);
    }
}
