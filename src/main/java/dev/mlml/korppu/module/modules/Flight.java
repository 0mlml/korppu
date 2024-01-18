package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.screens.FlightConfigScreen;
import dev.mlml.korppu.module.Module;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Flight extends Module
{
    public Mode mode = Mode.Ability;
    public double speed = 1.0;
    private boolean wasFlying = false;

    public Flight()
    {
        super("Flight", "Allows you to fly", ModuleType.PLAYER, GLFW.GLFW_KEY_Z);
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
    public void onTick()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }

        switch (mode)
        {
            case Ability:
                KorppuMod.mc.player.getAbilities().setFlySpeed((float) speed);
                KorppuMod.mc.player.getAbilities().allowFlying = true;
                break;
            case Velocity:
                GameOptions options = KorppuMod.mc.options;
                float yaw = KorppuMod.mc.player.getYaw();
                int dx = Boolean.compare(options.rightKey.isPressed(), options.leftKey.isPressed());
                int dy = Boolean.compare(options.jumpKey.isPressed(), options.sneakKey.isPressed());
                int dz = Boolean.compare(options.backKey.isPressed(), options.forwardKey.isPressed());

                double s = Math.sin(Math.toRadians(yaw));
                double c = Math.cos(Math.toRadians(yaw));

                double nx = speed * (dz * s + dx * -c);
                double nz = speed * (dz * -c + dx * -s);
                double ny = speed * dy;

                KorppuMod.mc.player.setVelocity(new Vec3d(nx, ny, nz));
                break;
        }

        if (mode != Mode.Ability)
        {
            KorppuMod.mc.player.getAbilities().allowFlying = wasFlying;
        }
    }

    public void onEnable()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }
        wasFlying = KorppuMod.mc.player.getAbilities().flying;
    }

    @Override
    public void onDisable()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }

        if (mode == Mode.Ability)
        {
            KorppuMod.mc.player.getAbilities().setFlySpeed(0.05f);
            KorppuMod.mc.player.getAbilities().allowFlying = wasFlying;
        }
    }

    @Override
    public void handleShiftPress()
    {
        KorppuMod.mc.setScreen(new FlightConfigScreen(this));
    }

    public enum Mode
    {
        Ability,
        Velocity;

        public Text getSimpleTranslatableName()
        {
            return Text.literal(this.name());
        }

        public String getIntial()
        {
            return this.name().substring(0, 1);
        }
    }
}
