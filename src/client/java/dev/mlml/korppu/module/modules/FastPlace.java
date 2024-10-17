package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.module.Module;
import org.lwjgl.glfw.GLFW;

public class FastPlace extends Module {
    public FastPlace() {
        super("FastPlace", "Place blocks faster", GLFW.GLFW_KEY_RIGHT_BRACKET);
    }
}
