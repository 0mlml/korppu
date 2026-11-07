package dev.mlml.korppu.imgui.windows;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;

import java.util.ArrayList;
import java.util.List;

public class LocateResultsWindow extends ImGuiWindow {
    private final List<String> entries = new ArrayList<>();
    private final ImBoolean openBool = new ImBoolean(true);
    private String pendingQuery = "";

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(520, 260, ImGuiCond.Once);
        ImGui.begin("Locate Results##korppu_locate", openBool);

        if (!pendingQuery.isBlank()) {
            ImGui.textDisabled("Pending: " + pendingQuery);
        }

        if (ImGui.button("Clear")) {
            entries.clear();
        }

        ImGui.separator();

        if (entries.isEmpty()) {
            ImGui.textDisabled("(no results)");
        } else {
            int start = Math.max(0, entries.size() - 100);
            if (start > 0) {
                ImGui.textDisabled("... " + start + " older result(s) hidden");
            }
            for (int i = start; i < entries.size(); i++) {
                ImGui.textWrapped(entries.get(i));
            }
        }

        ImGui.end();
        if (!openBool.get()) {
            visible = false;
        }
    }

    public void addEntry(String entry) {
        entries.add(entry);
    }

    public void setPendingQuery(String query) {
        pendingQuery = query == null ? "" : query;
    }

    public void clearPendingQuery() {
        pendingQuery = "";
    }
}
