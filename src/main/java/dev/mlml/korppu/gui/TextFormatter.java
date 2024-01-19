package dev.mlml.korppu.gui;

import dev.mlml.korppu.KorppuMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextFormatter
{
    public static String cformat(String text, Code... codes)
    {
        Map<Integer, String> codeMap = new HashMap<>();
        for (int i = 0; i < codes.length; i++)
        {
            codeMap.put(i + 1, codes[i].getCode());
        }

        for (Map.Entry<Integer, String> entry : codeMap.entrySet())
        {
            text = text.replace("%" + entry.getKey(), entry.getValue());
        }

        return text + Code.RESET.getCode();
    }

    public static String format(String text, Object... args)
    {
        // Split args into color codes and other args
        List<Code> fmtCodes = new ArrayList<>();
        List<String> fmtArgs = new ArrayList<>();

        for (Object arg : args)
        {
            if (arg instanceof Code)
            {
                fmtCodes.add((Code) arg);
            } else
            {
                fmtArgs.add(arg.toString());
            }
        }

        String formatted = cformat(text, fmtCodes.toArray(new Code[0]));
        try
        {
            return String.format(formatted, fmtArgs.toArray());
        } catch (Exception e)
        {
            return formatted;
        }
    }

    public enum Code
    {
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

        Code(String code)
        {
            this.code = code;
        }

        public String getCode()
        {
            return "§" + code;
        }
    }

}