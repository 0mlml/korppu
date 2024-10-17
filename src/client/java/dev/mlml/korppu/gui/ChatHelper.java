package dev.mlml.korppu.gui;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ChatHelper {
    public static final String PREFIX = TextFormatter.format("[%2%3Korppu%1] ", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY);

    public static void message(String... message) {
        MutableText prefix = Text.literal(PREFIX);

        for (String m : message) {
            KorppuMod.mc.inGameHud.getChatHud().addMessage(prefix.append(Text.literal(m)));
        }
    }
}
