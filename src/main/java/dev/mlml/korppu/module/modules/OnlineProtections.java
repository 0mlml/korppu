package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.module.Module;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

@Getter
public class OnlineProtections extends Module
{
    public OnlineProtections()
    {
        super("OnlineProtections", "Protects you from known exploits", ModuleType.META, GLFW.GLFW_KEY_UNKNOWN);
    }

    private boolean antiPacketKick = false;
}
