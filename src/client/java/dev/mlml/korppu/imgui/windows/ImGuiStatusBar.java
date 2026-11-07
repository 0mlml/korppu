package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.imgui.KorppuImGui;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;

public class ImGuiStatusBar extends ImGuiWindow {

    @Override
    public void render() {
        if (!KorppuImGui.shouldReleaseMouse()) return;

        float displayW = ImGui.getIO().getDisplaySizeX();
        ImGui.setNextWindowPos(displayW * 0.5f, 0, ImGuiCond.Always, 0.5f, 0f);
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.AlwaysAutoResize;

        ImGui.begin("##korppu_statusbar", flags);

        if (ImGui.smallButton(KorppuImGui.isMenuOpen() ? "Close Menu" : "Open Menu")) KorppuImGui.toggleMenu();
        ImGui.sameLine();
        ImGui.textDisabled("|");
        ImGui.sameLine();
        if (ImGui.smallButton("Hide All"))        KorppuImGui.hideAll();
        ImGui.sameLine();
        if (ImGui.smallButton("Hide Non-Status")) KorppuImGui.hideNonStatus();
        ImGui.sameLine();
        if (ImGui.smallButton("Presets"))         KorppuImGui.openLayoutPresets();

        ImGui.end();
    }
}
