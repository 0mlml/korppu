package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.imgui.WindowLayoutPreset;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.List;

public class LayoutPresetsWindow extends ImGuiWindow {
    private final ImString nameInput = new ImString(64);
    private final ImBoolean open = new ImBoolean(true);
    private final List<WindowLayoutPreset> presets;

    public LayoutPresetsWindow(List<WindowLayoutPreset> presets) {
        this.presets = presets;
    }

    @Override
    public void render() {
        if (!visible) return;
        open.set(true);

        ImGui.setNextWindowSize(340, 0, ImGuiCond.Once);
        if (ImGui.begin("Profiles##layout_presets", open, ImGuiWindowFlags.AlwaysAutoResize)) {

            ImGui.text("Config file (korppu.txt):");
            if (ImGui.button("Save##cfg_save")) {
                ConfigWriter.writeConfigToFile();
            }
            ImGui.sameLine();
            if (ImGui.button("Load##cfg_load")) {
                ConfigWriter.readConfigFromFile();
            }
            ImGui.separator();

            ImGui.text("Save profile (layout + module settings):");
            ImGui.setNextItemWidth(190);
            ImGui.inputText("##preset_name", nameInput);
            ImGui.sameLine();
            if (ImGui.button("Save##preset_save")) {
                String n = nameInput.get().trim();
                if (!n.isEmpty()) {
                    WindowLayoutPreset preset = KorppuImGui.capturePreset(n);
                    presets.removeIf(p -> p.name.equals(n));
                    presets.add(preset);
                    WindowLayoutPreset.save(presets);
                    nameInput.set("");
                }
            }

            ImGui.separator();

            if (presets.isEmpty()) {
                ImGui.textDisabled("No saved profiles.");
            } else {
                ImGui.text("Saved profiles:");
                for (int i = presets.size() - 1; i >= 0; i--) {
                    WindowLayoutPreset p = presets.get(i);
                    ImGui.pushID(i);
                    ImGui.text(p.name);
                    ImGui.sameLine();
                    if (ImGui.smallButton("Load")) {
                        KorppuImGui.applyPreset(p);
                    }
                    ImGui.sameLine();
                    if (ImGui.smallButton("Delete")) {
                        presets.remove(i);
                        WindowLayoutPreset.save(presets);
                    }
                    ImGui.popID();
                }
            }
        }
        ImGui.end();
        if (!open.get()) visible = false;
    }
}
