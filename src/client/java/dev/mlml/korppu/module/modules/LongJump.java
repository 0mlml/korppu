package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.Module;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

public class LongJump extends Module {
    public LongJump() {
        super("LongJump", "Jumps longer", GLFW.GLFW_KEY_K);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null) {
            return;
        }

       if (KorppuMod.mc.options.jumpKey.isPressed()) {
           KorppuMod.mc.player.jump();
       }
    }
}
