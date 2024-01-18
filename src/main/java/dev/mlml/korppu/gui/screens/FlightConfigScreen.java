package dev.mlml.korppu.gui.screens;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.ConfigScreen;
import dev.mlml.korppu.module.modules.Flight;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

// TODO: Look at how carpet does these
@Environment(EnvType.CLIENT)
public class FlightConfigScreen extends ConfigScreen
{
    private final TextFieldWidget speedField;
    private final CyclingButtonWidget<Flight.Mode> modeButton;
    private final Flight module;

    public FlightConfigScreen(Flight m)
    {
        super("KorppuMod Flight Config");

        module = m;
        speedField = new TextFieldWidget(KorppuMod.mc.textRenderer, 0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, Text.literal(String.format("Speed: %.2f", module.speed)));
        speedField.setTextPredicate(s -> s.matches("^[0-9]+.?[0-9]*$"));
        speedField.setChangedListener(s ->
        {
            try
            {
                module.speed = Math.max(0.1, Double.parseDouble(s));
            } catch (Exception e)
            {
                module.speed = 1.0;
            }

            speedField.setText(String.format("Speed: %.2f", module.speed));
        });

        modeButton = CyclingButtonWidget.builder(Flight.Mode::getSimpleTranslatableName)
                .values(Flight.Mode.values())
                .initially(module.mode)
                .build(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, module.mode.getSimpleTranslatableName(), (button, mode) ->
                {
                    module.mode = mode;
                });
    }

    @Override
    protected List<ClickableWidget> makeElements()
    {
        return List.of(speedField, modeButton);
    }
}
