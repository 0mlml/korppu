package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;

public class Say extends Command {
    public Say() {
        super("say", "Say something in chat", "s");
    }

    @Override
    public void execute(String[] args) {
        StringBuilder message = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }

        if (KorppuMod.mc.getNetworkHandler() == null) {
            return;
        }

        KorppuMod.mc.getNetworkHandler().sendChatMessage(message.toString());
    }
}
