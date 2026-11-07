package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorSigns extends Module {
    private static final Pattern AMP_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private final BooleanSetting rewriteAmpersand = config.add(new BooleanSetting(
            "Rewrite Ampersand",
            "Replace & color/style codes with section sign before sending",
            true
    ));

    private int rewrittenLines;

    public ColorSigns() {
        super("ColorSigns", "Allows & color formatting in sign text packets", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        rewrittenLines = 0;
    }

    @Listener
    public void onPacket(PacketEvent.Sent event) {
        if (!isEnabled() || !rewriteAmpersand.getValue() || event.getPacket() == null) return;
        if (!event.getPacket().getClass().getSimpleName().contains("UpdateSign")) return;

        for (Field field : event.getPacket().getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(event.getPacket());
                if (value == null) continue;

                if (value instanceof String str) {
                    String rewritten = rewrite(str);
                    if (!rewritten.equals(str)) {
                        field.set(event.getPacket(), rewritten);
                        rewrittenLines++;
                    }
                } else if (value.getClass().isArray() && value.getClass().getComponentType() == String.class) {
                    int len = Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        String line = (String) Array.get(value, i);
                        if (line == null) continue;
                        String rewritten = rewrite(line);
                        if (!rewritten.equals(line)) {
                            Array.set(value, i, rewritten);
                            rewrittenLines++;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static String rewrite(String in) {
        Matcher matcher = AMP_CODE.matcher(in);
        return matcher.replaceAll("\u00A7$1");
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Rewrite Amp", rewriteAmpersand.getValue()),
                StatusEntry.text("Rewritten", String.valueOf(rewrittenLines))
        );
    }
}
