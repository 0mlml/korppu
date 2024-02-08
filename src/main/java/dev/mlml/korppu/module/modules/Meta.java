package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.config.StringSetting;
import dev.mlml.korppu.module.Module;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class Meta extends Module {
    @Getter
    private final StringSetting prefix = config.add(new StringSetting("Prefix", "Prefix for commands", ";"));

    public Meta() {
        super("Meta", "Client settings", ModuleType.META, GLFW.GLFW_KEY_UNKNOWN);
    }
}
