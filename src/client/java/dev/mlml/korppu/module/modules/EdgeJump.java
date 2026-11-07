package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.module.Module;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

public class EdgeJump extends Module {
    private int displayTicks = 10;

    public EdgeJump() {
        super("EdgeJump", "Jumps on the edge of blocks",GLFW.GLFW_KEY_J);
    }

    @Override
    public void onTick() {
        if (displayTicks < 10) {
            displayTicks++;
        }
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }
        if (!KorppuMod.mc.player.onGround() || KorppuMod.mc.player.isCrouching()) {
            return;
        }

        AABB bounding = KorppuMod.mc.player.getBoundingBox();
        bounding = bounding.move(0, -0.5, 0);
        bounding = bounding.inflate(-0.001, 0, -0.001);
        if (!KorppuMod.mc.level.getBlockCollisions(KorppuMod.mc.player, bounding).iterator().hasNext()) {
            KorppuMod.mc.player.jumpFromGround();
            displayTicks = 0;
        }
    }

    @Override
    public String getStatus() {
        return displayTicks < 10 ? TextFormatter.format("%1Last: %s", TextFormatter.Code.WHITE, displayTicks) : "";
    }
}
