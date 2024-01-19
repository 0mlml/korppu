package dev.mlml.korppu.gui;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.GenericSetting;
import dev.mlml.korppu.module.Module;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen
{
    public static final int DEFAULT_WIDTH = 150;
    public static final int DEFAULT_HEIGHT = 20;
    public static final int GAPS = 4;

    private static final int MAX_WIDTH = KorppuMod.mc.getWindow().getScaledWidth() / 2;

    @Getter
    private final Module module;
    List<SettingLabel> texts = new ArrayList<>();

    public ConfigScreen(Module module)
    {
        super(Text.literal(module.getName()));
        this.module = module;
    }

    @Override
    public void init()
    {
        super.init();

        int x = GAPS;
        int y = GAPS;

        List<GenericSetting<?>> settings = module.getConfig().getSettings();

        int maxHeight = 0;
        for (GenericSetting<?> s : settings)
        {
            ClickableWidget e = s.getAsWidget();

            if (x + e.getWidth() > MAX_WIDTH)
            {
                x = GAPS;
                y += maxHeight + GAPS;
                maxHeight = 0;
            }

            texts.add(new SettingLabel(s.getLabel(), x, y));

            if (e.getHeight() + textRenderer.fontHeight + GAPS / 2 > maxHeight)
            {
                maxHeight = e.getHeight() + textRenderer.fontHeight + GAPS / 2;
            }

            e.setPosition(x, y + textRenderer.fontHeight + GAPS / 2);

            x += e.getWidth() + GAPS;

            addDrawableChild(e);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.render(context, mouseX, mouseY, delta);
        String s = module.getName() + " config screen";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(s), width - textRenderer.getWidth(s) / 2 - GAPS, GAPS, 0xffffff);

        for (SettingLabel t : texts)
        {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(t.text), t.x + textRenderer.getWidth(t.text) / 2, t.y, 0xffffff);
        }
    }

    private static class SettingLabel
    {
        String text;
        int x;
        int y;

        public SettingLabel(String text, int x, int y)
        {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }
}
