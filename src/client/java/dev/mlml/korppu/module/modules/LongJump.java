package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.Module;
import org.lwjgl.glfw.GLFW;

public class LongJump extends Module {
    public LongJump() {
        super("LongJump", "Jumps longer", GLFW.GLFW_KEY_K);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        if (KorppuMod.mc.options.keyJump.isDown()) {
            KorppuMod.mc.player.jumpFromGround();
        }
    }
}
