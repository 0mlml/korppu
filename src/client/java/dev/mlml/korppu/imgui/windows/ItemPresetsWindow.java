package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.item.ItemPreset;
import dev.mlml.korppu.item.ItemPresetPlaceholder;
import dev.mlml.korppu.item.ItemPresets;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemPresetsWindow extends ImGuiWindow {
    private static final String CONFIG_POPUP_ID = "Configure Preset##preset_cfg";

    private final ImBoolean openBool = new ImBoolean(true);
    private final ImBoolean popupOpen = new ImBoolean(true);
    private ItemPreset configuring;
    private final Map<String, ImString> placeholderInputs = new HashMap<>();

    @Override
    public WindowType getWindowType() { return WindowType.MENU; }

    @Override
    public void render() {
        if (!visible) {
            renderConfigurePopup();
            return;
        }

        openBool.set(true);
        ImGui.setNextWindowSize(480, 400, ImGuiCond.Once);
        if (!ImGui.begin("Item Presets##korppu_presets", openBool, ImGuiWindowFlags.None)) {
            ImGui.end();
            renderConfigurePopup();
            if (!openBool.get()) visible = false;
            return;
        }

        ImGui.textDisabled("Click Generate to preview in the item modal. Use Spawn or Save to Registry from there.");
        ImGui.separator();

        if (ImGui.beginTable("##presets_table", 3,
                ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.RowBg |
                ImGuiTableFlags.ScrollY | ImGuiTableFlags.BordersInnerH,
                0, ImGui.getContentRegionAvailY())) {

            ImGui.tableSetupScrollFreeze(0, 1);
            ImGui.tableSetupColumn("Name",        0, 0.30f);
            ImGui.tableSetupColumn("Description", 0, 0.55f);
            ImGui.tableSetupColumn("",            0, 0.15f);
            ImGui.tableHeadersRow();

            for (int i = 0; i < ItemPresets.ALL.size(); i++) {
                ItemPreset preset = ItemPresets.ALL.get(i);
                ImGui.tableNextRow();

                ImGui.tableSetColumnIndex(0);
                ImGui.text(preset.name());

                ImGui.tableSetColumnIndex(1);
                ImGui.textWrapped(preset.description());

                ImGui.tableSetColumnIndex(2);
                if (ImGui.smallButton("Generate##preset_" + i)) {
                    if (preset.configurable()) {
                        configuring = preset;
                        placeholderInputs.clear();
                        popupOpen.set(true);
                        ImGui.openPopup(CONFIG_POPUP_ID);
                    } else {
                        ItemStack stack = preset.generate();
                        if (!stack.isEmpty()) {
                            ItemModal.openForStack(stack);
                        }
                    }
                }
            }

            ImGui.endTable();
        }

        ImGui.end();
        renderConfigurePopup();
        if (!openBool.get()) visible = false;
    }

    private void renderConfigurePopup() {
        if (configuring == null) return;

        ImGui.setNextWindowSize(420, 0, ImGuiCond.Appearing);
        if (!ImGui.beginPopupModal(CONFIG_POPUP_ID, popupOpen, ImGuiWindowFlags.AlwaysAutoResize)) {
            if (!ImGui.isPopupOpen(CONFIG_POPUP_ID)) {
                configuring = null;
                placeholderInputs.clear();
            }
            return;
        }

        if (!popupOpen.get()) {
            ImGui.closeCurrentPopup();
            configuring = null;
            placeholderInputs.clear();
            ImGui.endPopup();
            return;
        }

        ImGui.textWrapped(configuring.name());
        ImGui.textDisabled(configuring.description());
        ImGui.separator();

        for (ItemPresetPlaceholder placeholder : configuring.placeholders()) {
            ImGui.text(placeholder.label());
            ImGui.pushItemWidth(-1);
            ImGui.inputText("##cfg_" + placeholder.key(), placeholderInputs.computeIfAbsent(
                    placeholder.key(),
                    k -> new ImString(placeholder.defaultValue(), 1024)
            ));
            ImGui.popItemWidth();
        }

        if (ImGui.button("Generate##preset_cfg_generate")) {
            Map<String, String> values = new HashMap<>();
            for (ItemPresetPlaceholder p : configuring.placeholders()) {
                ImString input = placeholderInputs.get(p.key());
                values.put(p.key(), input != null ? input.get() : p.defaultValue());
            }
            ItemStack stack = configuring.generate(values);
            if (!stack.isEmpty()) {
                ItemModal.openForStack(stack);
                ImGui.closeCurrentPopup();
                popupOpen.set(false);
                configuring = null;
                placeholderInputs.clear();
            }
        }
        ImGui.sameLine();
        if (ImGui.button("Cancel##preset_cfg_cancel")) {
            ImGui.closeCurrentPopup();
            popupOpen.set(false);
            configuring = null;
            placeholderInputs.clear();
        }

        ImGui.endPopup();
    }
}
