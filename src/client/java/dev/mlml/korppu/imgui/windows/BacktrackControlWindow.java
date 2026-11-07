package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.module.modules.Backtrack;
import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class BacktrackControlWindow extends ModuleControlWindow {
    private final Backtrack backtrack;

    public BacktrackControlWindow(Backtrack backtrack) {
        super(backtrack);
        this.backtrack = backtrack;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(300, 230, ImGuiCond.Once);
        ImGui.begin(getTitle() + "##btctrl", openBool);

        ModuleStatusWindow.renderEntries(backtrack.getStatusEntries());

        ImGui.separator();

        if (ImGui.button("Seek Back")) backtrack.seekBack();
        ImGui.sameLine();
        if (ImGui.button("Replay")) backtrack.startCatchingUp();
        ImGui.sameLine();
        if (ImGui.button("Stop")) backtrack.stopPlayback();

        ImGui.textDisabled("Ctx1=SeekBack  Ctx2=Replay  Ctx3=Stop");

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
        }
    }

    @Override
    public void onContextKey(int index) {
        switch (index) {
            case 0 -> backtrack.seekBack();
            case 1 -> backtrack.startCatchingUp();
            case 2 -> backtrack.stopPlayback();
        }
    }
}
