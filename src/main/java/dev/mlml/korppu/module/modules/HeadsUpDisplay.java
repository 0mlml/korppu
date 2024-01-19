package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HeadsUpDisplay extends Module
{
    private final BooleanSetting showWatermark = config.add(new BooleanSetting("Show Watermark", "Shows the Korppu watermark", true));
    // TODO
    // private final MultiListSetting<Something> exclusions = config.add(new MultiListSetting<>("Exclusions", "Excludes modules from being displayed", Something.class));

    public HeadsUpDisplay()
    {
        super("HeadsUpDisplay", "Displays information on your screen", ModuleType.RENDER, GLFW.GLFW_KEY_EQUAL);
    }

    private void renderText(DrawContext drawContext, String text, Corner corner, int index)
    {
        int[] base = corner.getBase(KorppuMod.mc.textRenderer.getWidth(text), KorppuMod.mc.textRenderer.fontHeight, index);

        drawContext.drawCenteredTextWithShadow(KorppuMod.mc.textRenderer, text, base[0], base[1], 0xFFFF);
    }

    @Override
    public void onRender(DrawContext drawContext, float tickDelta)
    {
        if (!isEnabled())
        {
            return;
        }

        List<Module> modules = ModuleManager.getModules();

        for (Module m : modules)
        {
            String status = m.getStatus();

            String format = TextFormatter.format("%2%3[%4%s%3]%1 %5%s%s",
                    TextFormatter.Code.RESET,
                    TextFormatter.Code.BOLD,
                    TextFormatter.Code.GRAY,
                    TextFormatter.Code.WHITE,
                    m.isEnabled() ? TextFormatter.Code.GREEN : TextFormatter.Code.RED,
                    m.getKeybind().getBoundKeyLocalizedText().getString(),
                    m.getName(),
                    status.isEmpty() ? "" : " : " + status);

            renderText(drawContext, format, Corner.TOP_LEFT, modules.indexOf(m));
        }

        if (!showWatermark.getValue())
        {
            return;
        }
        renderText(drawContext, TextFormatter.format("[%2%3Korppu%1 v%2%3%s%1] %s FPS",
                TextFormatter.Code.RESET,
                TextFormatter.Code.BOLD,
                TextFormatter.Code.GRAY,
                KorppuMod.VERSION,
                KorppuMod.mc.getCurrentFps()
        ), Corner.TOP_RIGHT, 0);
    }

    @Override
    public String getStatus()
    {
        int enabled = ModuleManager.getModules().stream().filter(Module::isEnabled).toArray().length;

        return TextFormatter.format("%1%s|%2%s",
                TextFormatter.Code.GREEN,
                enabled,
                TextFormatter.Code.RED,
                ModuleManager.getModules().size() - enabled);
    }

    private enum Corner
    {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public int[] getBase(int textWidth, int textHeight, int row)
        {
            int gap = 2;

            return switch (this)
            {
                case TOP_LEFT -> new int[]{gap + textWidth / 2, gap + (textHeight + gap) * row};
                case TOP_RIGHT ->
                        new int[]{KorppuMod.mc.getWindow().getScaledWidth() - textWidth / 2 - gap, gap + (textHeight + gap) * row};
                case BOTTOM_LEFT ->
                        new int[]{gap + textWidth / 2, KorppuMod.mc.getWindow().getScaledHeight() + gap + (textHeight + gap) * row};
                case BOTTOM_RIGHT ->
                        new int[]{KorppuMod.mc.getWindow().getScaledWidth() - textWidth / 2 - gap, KorppuMod.mc.getWindow().getScaledHeight() + gap + (textHeight + gap) * row};
            };
        }
    }
}
