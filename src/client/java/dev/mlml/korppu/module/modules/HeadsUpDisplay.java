package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.gui.ConfigScreen;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HeadsUpDisplay extends Module {
    private final BooleanSetting showWatermark = config.add(new BooleanSetting("Show Watermark", "Shows the Korppu watermark", true));
    private final BooleanSetting showFPS = config.add(new BooleanSetting("Show FPS", "Shows your current FPS", true));
    private final BooleanSetting showCoordinates = config.add(new BooleanSetting("Show Coordinates", "Shows your current coordinates", false));
    private final BooleanSetting showVelocity = config.add(new BooleanSetting("Show Velocity", "Shows your current velocity", false));

    // TODO
    // private final MultiListSetting<Something> exclusions = config.add(new MultiListSetting<>("Exclusions", "Excludes modules from being displayed", Something.class));
    private final BooleanSetting onlyShowEnabled = config.add(new BooleanSetting("Only Show Enabled", "Only show enabled modules", false));
    private final BooleanSetting simplify = config.add(new BooleanSetting("Simplify", "Simplify the HUD", false));

    private final BooleanSetting keyControls = config.add(new BooleanSetting("Key Controls", "Allow controlling with keys", true));
    public final KeyBinding toggleKey = new KeyBinding("key.korppumod.hud_toggle", GLFW.GLFW_KEY_LEFT, "category.korppumod");
    public final KeyBinding upKey = new KeyBinding("key.korppumod.hud_up", GLFW.GLFW_KEY_UP, "category.korppumod");
    public final KeyBinding downKey = new KeyBinding("key.korppumod.hud_down", GLFW.GLFW_KEY_DOWN, "category.korppumod");
    public final KeyBinding settingsKey = new KeyBinding("key.korppumod.hud_settings", GLFW.GLFW_KEY_RIGHT, "category.korppumod");

    private int kcSelection = 0;
    private long kcShowingTicks = 0;
    private Module kcSelected = null;

    public HeadsUpDisplay() {
        super("HeadsUpDisplay", "Displays information on your screen", GLFW.GLFW_KEY_EQUAL);

        KeyBindingHelper.registerKeyBinding(toggleKey);
        KeyBindingHelper.registerKeyBinding(upKey);
        KeyBindingHelper.registerKeyBinding(downKey);
        KeyBindingHelper.registerKeyBinding(settingsKey);
    }

    private void renderText(DrawContext drawContext, String text, Corner corner, int index) {
        int[] base = corner.getBase(KorppuMod.mc.textRenderer.getWidth(text), KorppuMod.mc.textRenderer.fontHeight, index);

        drawContext.drawCenteredTextWithShadow(KorppuMod.mc.textRenderer, text, base[0], base[1], 0xFFFF);
    }

    @Override
    public void onTick() {
        if (kcShowingTicks > 0) {
            kcShowingTicks--;
        }

        if (!keyControls.getValue() || simplify.getValue()) {
            return;
        }

        boolean isVisible = kcShowingTicks > 0;

        if (toggleKey.wasPressed()) {
            if (isVisible && kcSelected != null) {
                kcSelected.toggle();
            }
            kcShowingTicks = 100;
        }

        if (upKey.wasPressed()) {
            if (isVisible) {
                kcSelection--;
                if (kcSelection < 0) {
                    kcSelection = ModuleManager.getModules().size() - 1;
                }
            }
            kcShowingTicks = 100;
        }

        if (downKey.wasPressed()) {
            if (isVisible) {
                kcSelection++;
                if (kcSelection >= ModuleManager.getModules().size()) {
                    kcSelection = 0;
                }
            }
            kcShowingTicks = 100;
        }

        if (settingsKey.wasPressed()) {
            if (isVisible && kcSelected != null) {
                KorppuMod.mc.setScreen(new ConfigScreen(kcSelected));
            }
            kcShowingTicks = 100;
        }
    }

    @Override
    public void onRender(DrawContext drawContext, RenderTickCounter tickDelta) {
        if (!isEnabled()) {
            return;
        }

        List<Module> modules = ModuleManager.getModules();

        int count = 0;
        if (!simplify.getValue()) {
            for (Module m : modules) {
                if (onlyShowEnabled.getValue() && !m.isEnabled()) {
                    continue;
                }

                String status = m.getStatus();
                String selection = "";
                if (kcSelection == count && kcShowingTicks > 0) {
                    kcSelected = m;
                    selection = TextFormatter.format("%2%3>%1 ", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GOLD);
                }

                String format = TextFormatter.format("%2%3[%4%s%3]%1 %5%s%s", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, TextFormatter.Code.WHITE, m.isEnabled() ? TextFormatter.Code.GREEN : TextFormatter.Code.RED, m.getKeybind().getBoundKeyLocalizedText().getString(), m.getName(), status.isEmpty() ? "" : " : " + status);

                renderText(drawContext, selection + format, Corner.TOP_LEFT, count);
                count++;
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(TextFormatter.format("%1", TextFormatter.Code.RESET));
            int width = 0;
            for (Module m : modules) {
                if (onlyShowEnabled.getValue() && !m.isEnabled()) {
                    continue;
                }

                sb.append(TextFormatter.format("%2%s%1, ", TextFormatter.Code.WHITE, m.getName(), m.isEnabled() ? TextFormatter.Code.GREEN : TextFormatter.Code.RED));
                width += KorppuMod.mc.textRenderer.getWidth(m.getName() + ", ");

                if (width > KorppuMod.mc.getWindow().getScaledWidth() / 3) {
                    renderText(drawContext, sb.toString(), Corner.TOP_LEFT, count);
                    count++;
                    sb = new StringBuilder();
                    width = 0;
                }
            }

            if (sb.toString().isEmpty()) {
                return;
            }

            sb.delete(sb.length() - 2, sb.length());
            renderText(drawContext, sb.toString(), Corner.TOP_LEFT, count);
        }

        if (!showWatermark.getValue()) {
            return;
        }
        renderText(drawContext, TextFormatter.format("[%2%3Korppu%1 v%2%3%s%1]", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.VERSION), Corner.TOP_RIGHT, 0);
        count = 1;
        if (showFPS.getValue()) {
            renderText(drawContext, TextFormatter.format("FPS: %2%3%d", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.mc.getCurrentFps()), Corner.TOP_RIGHT, count++);
        }
        if (showCoordinates.getValue()) {
            renderText(drawContext, TextFormatter.format("X: %2%3%.2f%1 Y: %2%3%.2f%1 Z: %2%3%.2f", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY(), KorppuMod.mc.player.getZ()), Corner.TOP_RIGHT, count++);
        }
        if (showVelocity.getValue()) {
            renderText(drawContext, TextFormatter.format("Vel: %2%3%.2f", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.mc.player.getVelocity().length()), Corner.TOP_RIGHT, count++);
        }
    }

    @Override
    public String getStatus() {
        int enabled = ModuleManager.getModules().stream().filter(Module::isEnabled).toArray().length;

        return TextFormatter.format("%1%s | %2%s", TextFormatter.Code.GREEN, enabled, TextFormatter.Code.RED, ModuleManager.getModules().size() - enabled);
    }

    private enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public int[] getBase(int textWidth, int textHeight, int row) {
            int gap = 2;

            return switch (this) {
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
