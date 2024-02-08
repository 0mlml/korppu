package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.module.Module;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

@Getter
public class OnlineProtections extends Module {
    private final boolean antiPacketKick = false;

    public OnlineProtections() {
        super("OnlineProtections", "Protects you from known exploits", ModuleType.META, GLFW.GLFW_KEY_UNKNOWN);
    }
}
