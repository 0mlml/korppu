package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.config.LiveConfig;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;

import java.util.List;

public class UncleanShutdownWindow extends ImGuiWindow {
    private final ImBoolean openBool = new ImBoolean(true);
    private List<LiveConfig.Change> changes = List.of();

    public void open(List<LiveConfig.Change> changes) {
        this.changes = changes;
        show();
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(640, 380, ImGuiCond.Once);
        ImGui.setNextWindowPos(ImGui.getIO().getDisplaySizeX() * 0.5f, ImGui.getIO().getDisplaySizeY() * 0.5f,
                ImGuiCond.Once, 0.5f, 0.5f);
        ImGui.begin("Last shutdown was not clean##unclean_shutdown", openBool);

        ImGui.textWrapped("Korppu did not shut down cleanly last time, so " + ConfigWriter.configFile
                + " was not updated. The changes below were made during that session and were recovered from "
                + LiveConfig.LIVE_FILE + ".");
        ImGui.separator();

        float footer = ImGui.getFrameHeightWithSpacing() + ImGui.getStyle().getItemSpacingY();
        if (changes.isEmpty()) {
            ImGui.textDisabled("No settings changed during the last session.");
        } else {
            int flags = ImGuiTableFlags.RowBg | ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.ScrollY
                    | ImGuiTableFlags.Resizable;
            if (ImGui.beginTable("##unclean_diff", 4, flags, 0, -footer)) {
                ImGui.tableSetupColumn("Module", ImGuiTableColumnFlags.WidthFixed, 130);
                ImGui.tableSetupColumn("Setting", ImGuiTableColumnFlags.WidthFixed, 130);
                ImGui.tableSetupColumn(ConfigWriter.configFile, ImGuiTableColumnFlags.WidthStretch);
                ImGui.tableSetupColumn("Last session", ImGuiTableColumnFlags.WidthStretch);
                ImGui.tableSetupScrollFreeze(0, 1);
                ImGui.tableHeadersRow();

                for (LiveConfig.Change change : changes) {
                    int slash = change.key().indexOf('/');
                    ImGui.tableNextRow();
                    ImGui.tableNextColumn();
                    ImGui.text(slash < 0 ? change.key() : change.key().substring(0, slash));
                    ImGui.tableNextColumn();
                    ImGui.text(slash < 0 ? "Enabled" : change.key().substring(slash + 1));
                    ImGui.tableNextColumn();
                    renderValue(change.saved());
                    ImGui.tableNextColumn();
                    renderValue(change.session());
                }
                ImGui.endTable();
            }
        }

        if (!changes.isEmpty() && ImGui.button("Restore last session")) {
            LiveConfig.restoreRecovered();
            visible = false;
        }
        if (!changes.isEmpty()) ImGui.sameLine();
        if (ImGui.button("Keep " + ConfigWriter.configFile)) {
            visible = false;
        }

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
        }
    }

    private static void renderValue(String value) {
        if (value == null) {
            ImGui.textDisabled("(missing)");
        } else if (value.isEmpty()) {
            ImGui.textDisabled("(empty)");
        } else {
            ImGui.textWrapped(value);
        }
    }
}
