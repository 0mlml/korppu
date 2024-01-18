package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.screens.FlightConfigScreen;
import dev.mlml.korppu.module.Module;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class Flight extends Module
{
    private final KeyBinding cycleKeybind;
    private final KeyBinding speedUpKeybind;
    private final KeyBinding speedDownKeybind;
    public Mode mode = Mode.Vanilla;
    public double speed = 1.0;

    public Flight()
    {
        super("Flight", "Allows you to fly", ModuleType.PLAYER, GLFW.GLFW_KEY_Z);

        cycleKeybind = new KeyBinding("key.korppumod.flight_cycle", GLFW.GLFW_KEY_UNKNOWN, "category.korppumod");
        KeyBindingHelper.registerKeyBinding(cycleKeybind);
        speedUpKeybind = new KeyBinding("key.korppumod.flight_speed_up", GLFW.GLFW_KEY_UNKNOWN, "category.korppumod");
        KeyBindingHelper.registerKeyBinding(speedUpKeybind);
        speedDownKeybind = new KeyBinding("key.korppumod.flight_speed_down", GLFW.GLFW_KEY_UNKNOWN, "category.korppumod");
        KeyBindingHelper.registerKeyBinding(speedDownKeybind);
    }

    public void cycleMode()
    {
        mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
    }

    public void speedUp()
    {
        speed += 0.1;
    }

    public void speedDown()
    {
        speed -= 0.1;
    }

    @Override
    public String getStatus()
    {
        return String.format("%s, %s", mode.getIntial(), speed);
    }

    @Override
    public void onTick() {
        if (cycleKeybind.wasPressed())
        {
            cycleMode();
        }
        if (speedUpKeybind.wasPressed())
        {
            speedUp();
        }
        if (speedDownKeybind.wasPressed())
        {
            speedDown();
        }
    }

    @Override
    public void handleShiftPress()
    {
        KorppuMod.mc.setScreen(new FlightConfigScreen(this));
    }

    public enum Mode
    {
        Vanilla,
        Packet;

        public Text getSimpleTranslatableName() {
            return Text.literal(this.getClass().getSimpleName());
        }

        public String getIntial()
        {
            return this.getClass().getSimpleName().substring(0, 1);
        }
    }
}
