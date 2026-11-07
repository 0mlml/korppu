package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.module.Module;
import imgui.type.ImBoolean;
import lombok.Getter;

public abstract class ModuleControlWindow extends ImGuiWindow {
    @Getter
    protected final Module module;
    protected final ImBoolean openBool = new ImBoolean(true);

    protected ModuleControlWindow(Module module) {
        this.module = module;
    }

    public String getTitle() {
        return module.getName() + " Control";
    }

    public void onContextKey(int index) {}
}
