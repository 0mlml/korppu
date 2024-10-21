package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.module.Module;
import org.lwjgl.glfw.GLFW;

public class TPRange extends Module {
    public TPRange() {
        super("TPRange", "Teleport range", GLFW.GLFW_KEY_KP_3);
    }

    @Override
    public void onTick() {

    }
}
