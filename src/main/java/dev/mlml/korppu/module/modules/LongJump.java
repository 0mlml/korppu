package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.module.Module;
import org.lwjgl.glfw.GLFW;

public class LongJump extends Module {
    public LongJump() {
        super("LongJump", "Jumps longer", ModuleType.PLAYER, GLFW.GLFW_KEY_K);
    }
}
