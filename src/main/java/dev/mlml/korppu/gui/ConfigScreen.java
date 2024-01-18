package dev.mlml.korppu.gui;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ConfigScreen extends Screen
{
    public static final int DEFAULT_WIDTH = 150;
    public static final int DEFAULT_HEIGHT = 20;
    public static final int GAPS = 4;

    private static final int MAX_WIDTH = KorppuMod.mc.getWindow().getScaledWidth() / 2;

    public ConfigScreen(String name)
    {
        super(Text.literal(name));
    }

    protected List<ClickableWidget> makeElements()
    {
        return List.of();
    }

    @Override
    public void init()
    {
        super.init();

        int x = GAPS;
        int y = GAPS;

        List<ClickableWidget> elements = makeElements();

        int height = 0;
        for (ClickableWidget e : elements)
        {
            if (y > height)
            {
                height = y;
            }

            if (x + e.getWidth() > MAX_WIDTH)
            {
                x = GAPS;
                y += height + GAPS;
            }

            e.setPosition(x, y);

            x += e.getWidth() + GAPS;

            addDrawableChild(e);
        }
    }
}
