package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.module.Module;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

public class EdgeJump extends Module
{
    private int displayTicks = 0;

    public EdgeJump()
    {
        super("EdgeJump", "Jumps on the edge of blocks", ModuleType.PLAYER, GLFW.GLFW_KEY_J);
    }

    @Override
    public void onTick()
    {
        if (displayTicks > 0)
        {
            displayTicks--;
        }
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null)
        {
            return;
        }
        if (!KorppuMod.mc.player.isOnGround() || KorppuMod.mc.player.isSneaking())
        {
            return;
        }

        Box bounding = KorppuMod.mc.player.getBoundingBox();
        bounding = bounding.offset(0, -0.5, 0);
        bounding = bounding.expand(-0.001, 0, -0.001);
        if (!KorppuMod.mc.world.getBlockCollisions(KorppuMod.mc.player, bounding).iterator().hasNext())
        {
            KorppuMod.mc.player.jump();
            displayTicks = 10;
        }
    }

    @Override
    public String getStatus()
    {
        return displayTicks > 0 ? TextFormatter.format("%1Last: %s", TextFormatter.Code.WHITE, displayTicks) : "";
    }
}
