package dev.mlml.korppu.gui.screens;

import dev.mlml.korppu.gui.ConfigScreen;
import dev.mlml.korppu.module.modules.HeadsUpDisplay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class HeadsUpDisplayConfigScreen extends ConfigScreen
{
    private final HeadsUpDisplay module;
    private final ButtonWidget showWatermarkButton;

    public HeadsUpDisplayConfigScreen(HeadsUpDisplay m)
    {
        super("KorppuMod HeadsUpDisplay Config");

        this.module = m;

        showWatermarkButton = ButtonWidget.builder(Text.literal(String.format("Show Watermark: %s", module.showWatermark ? "yes" : "no")), button ->
                {
                    module.showWatermark = !module.showWatermark;
                    button.setMessage(Text.literal(String.format("Show Watermark: %s", module.showWatermark ? "yes" : "no")));
                })
                .dimensions(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT)
                .tooltip(Tooltip.of(Text.literal("Show Watermark")))
                .build();
    }

    @Override
    protected List<ClickableWidget> makeElements()
    {
        return List.of(showWatermarkButton);
    }
}
