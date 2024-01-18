package dev.mlml.korppu.gui.screens;

import dev.mlml.korppu.gui.ConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientConfigScreen extends ConfigScreen
{
    private final ButtonWidget button1;
    private final ButtonWidget button2;
    public ClientConfigScreen()
    {
        super("KorppuMod Config");

        button1 = ButtonWidget.builder(Text.literal("Button 1"), button ->
                {
                    System.out.println("You clicked button1!");
                })
                .dimensions(width / 2 - 205, 20, 200, 20)
                .tooltip(Tooltip.of(Text.literal("Tooltip of button1")))
                .build();
        button2 = ButtonWidget.builder(Text.literal("Button 2"), button ->
                {
                    System.out.println("You clicked button 2!");
                })
                .dimensions(width / 2 + 5, 20, 200, 20)
                .tooltip(Tooltip.of(Text.literal("Tooltip of button2")))
                .build();
    }

    @Override
    protected List<ClickableWidget> makeElements()
    {
        return List.of(button1, button2);
    }
}
