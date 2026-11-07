package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;

import java.util.List;

public class ModuleStatusWindow extends ImGuiWindow {
    private final Module module;
    private final ImBoolean openBool = new ImBoolean(true);

    public ModuleStatusWindow(Module module) {
        this.module = module;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(260, 200, ImGuiCond.Once);
        ImGui.begin(module.getName() + " Status##mstatus_" + module.getName(), openBool);

        List<StatusEntry> entries = module.getStatusEntries();
        renderEntries(entries);

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
        }
    }

    public static void renderEntries(List<StatusEntry> entries) {
        if (entries.isEmpty()) {
            ImGui.textDisabled("(no status entries)");
            return;
        }
        for (StatusEntry e : entries) {
            renderEntry(e);
        }
    }

    private static void renderEntry(StatusEntry entry) {
        switch (entry.type) {
            case TEXT -> {
                if (entry.color != -1) {
                    float a = ((entry.color >> 24) & 0xFF) / 255f;
                    float r = ((entry.color >> 16) & 0xFF) / 255f;
                    float g = ((entry.color >> 8) & 0xFF) / 255f;
                    float b = (entry.color & 0xFF) / 255f;
                    ImGui.textColored(r, g, b, a, entry.key + ": " + entry.value);
                } else {
                    ImGui.text(entry.key + ": " + entry.value);
                }
            }
            case BAR -> {
                ImGui.text(entry.key + ":");
                if (entry.color != -1) {
                    float a = ((entry.color >> 24) & 0xFF) / 255f;
                    float r = ((entry.color >> 16) & 0xFF) / 255f;
                    float g = ((entry.color >> 8) & 0xFF) / 255f;
                    float b = (entry.color & 0xFF) / 255f;
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.PlotHistogram, r, g, b, a);
                    ImGui.progressBar(entry.progress, -1f, 14f, entry.value);
                    ImGui.popStyleColor();
                } else {
                    ImGui.progressBar(entry.progress, -1f, 14f, entry.value);
                }
            }
            case INDICATOR -> {
                int c = entry.color;
                float a = ((c >> 24) & 0xFF) / 255f;
                float r = ((c >> 16) & 0xFF) / 255f;
                float g = ((c >> 8) & 0xFF) / 255f;
                float b = (c & 0xFF) / 255f;
                String bracket = "Y".equals(entry.value) ? "[*]" : "[ ]";
                ImGui.textColored(r, g, b, a, bracket + " " + entry.key);
            }
        }
    }
}
