package dev.mlml.korppu.gui;

import dev.mlml.korppu.KorppuMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextFormatter {
    /**
     * Formats a string by replacing placeholders with corresponding formatting codes.
     * Placeholders in the text should be in the format "%1", "%2", etc., where the number
     * corresponds to the position of the formatting code passed in the variable arguments.
     * The method automatically appends a reset formatting code at the end of the text.
     *
     * @param text  The text to be formatted, containing placeholders for formatting codes.
     * @param codes An array of {@link Code} enums representing formatting codes.
     * @return The formatted string with all placeholders replaced by the actual formatting codes,
     * followed by a reset formatting code.
     */
    public static String cformat(String text, Code... codes) {
        Map<Integer, String> codeMap = new HashMap<>();
        for (int i = 0; i < codes.length; i++) {
            codeMap.put(i + 1, codes[i].getCode());
        }

        for (Map.Entry<Integer, String> entry : codeMap.entrySet()) {
            text = text.replace("%" + entry.getKey(), entry.getValue());
        }

        return text + Code.RESET.getCode();
    }

    /**
     * Formats a string by first replacing placeholders for formatting codes, then using {@link String#format}
     * to insert other arguments into the resulting string. This method separates formatting codes from other
     * arguments, applying formatting codes first and then passing other arguments to String.format.
     * The formatting codes should be specified as enum constants of type {@link Code}, and placeholders
     * in the text for these codes should be "%1", "%2", etc. The method handles these separately from
     * placeholders for other arguments, which follow the standard {@link String#format} syntax ("%s", "%d", etc.).
     *
     * @param text The text to be formatted, containing placeholders for both formatting codes and other arguments.
     * @param args A mixed array of {@link Code} enums for formatting and other objects to be formatted into the string.
     *             Formatting codes will be applied first, followed by the insertion of other arguments.
     * @return The fully formatted string with formatting codes applied and other arguments inserted.
     * If an exception occurs during formatting with {@link String#format}, the partially formatted string
     * with formatting codes applied will be returned.
     */
    public static String format(String text, Object... args) {
        List<Code> fmtCodes = new ArrayList<>();
        List<Object> fmtArgs = new ArrayList<>();

        for (Object arg : args) {
            if (arg instanceof Code) {
                fmtCodes.add((Code) arg);
            } else {
                fmtArgs.add(arg);
            }
        }

        String formatted = cformat(text, fmtCodes.toArray(new Code[0]));
        try {
            return String.format(formatted, fmtArgs.toArray());
        } catch (Exception e) {
            return formatted;
        }
    }

    public enum Code {
        BLACK("0"),
        DARK_BLUE("1"),
        DARK_GREEN("2"),
        DARK_AQUA("3"),
        DARK_RED("4"),
        DARK_PURPLE("5"),
        GOLD("6"),
        GRAY("7"),
        DARK_GRAY("8"),
        BLUE("9"),
        GREEN("a"),
        AQUA("b"),
        RED("c"),
        LIGHT_PURPLE("d"),
        YELLOW("e"),
        WHITE("f"),
        OBFUSCATED("k"),
        BOLD("l"),
        STRIKETHROUGH("m"),
        UNDERLINE("n"),
        ITALIC("o"),
        RESET("r"),
        SECTION("§");

        private final String code;

        Code(String code) {
            this.code = code;
        }

        public String getCode() {
            return "§" + code;
        }
    }

}