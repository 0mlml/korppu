package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.module.modules.Aimbot;
import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class AimbotControlWindow extends ModuleControlWindow {
    private final Aimbot aimbot;

    public AimbotControlWindow(Aimbot aimbot) {
        super(aimbot);
        this.aimbot = aimbot;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(360, 260, ImGuiCond.Once);
        ImGui.begin(getTitle() + "##aimctrl", openBool);

        ModuleStatusWindow.renderEntries(aimbot.getStatusEntries());

        ImGui.separator();

        if (ImGui.button(aimbot.isTargetLocked() ? "Unlock" : "Lock")) {
            aimbot.toggleTargetLock();
        }
        ImGui.sameLine();
        if (ImGui.button("Previous")) {
            aimbot.cycleTarget(-1);
        }
        ImGui.sameLine();
        if (ImGui.button("Next")) {
            aimbot.cycleTarget(1);
        }

        ImGui.textDisabled("Ctx1=Lock/Unlock  Ctx2=Previous  Ctx3=Next");

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
        }
    }

    @Override
    public void onContextKey(int index) {
        switch (index) {
            case 0 -> aimbot.toggleTargetLock();
            case 1 -> aimbot.cycleTarget(-1);
            case 2 -> aimbot.cycleTarget(1);
        }
    }
}
