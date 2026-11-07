package dev.mlml.korppu.gui;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

public class ChatHelper {
    public static final String PREFIX = TextFormatter.format("[%2%3Korppu%1] ", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY);

    public static void message(String... message) {
        MutableComponent prefix = Component.literal(PREFIX);

        for (String m : message) {
            KorppuMod.mc.gui.getChat().addClientSystemMessage(prefix.copy().append(Component.literal(m)));
        }
    }
}
