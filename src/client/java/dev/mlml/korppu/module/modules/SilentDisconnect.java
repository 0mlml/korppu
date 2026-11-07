package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.module.Module;
import org.lwjgl.glfw.GLFW;

public class SilentDisconnect extends Module {
    public SilentDisconnect() {
        super("SilentDisconnect", "Suppresses disconnect screen and keeps you in-session view", GLFW.GLFW_KEY_UNKNOWN);
    }
}
