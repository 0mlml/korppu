package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.module.Category;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.List;
import java.util.Locale;

public class ModuleListWindow extends ImGuiWindow {
    @Override
    public WindowType getWindowType() { return WindowType.MENU; }

    private static final float SIDEBAR_WIDTH = 140f;
    private static final float DETAIL_WIDTH = 320f;

    private final ModuleConfigWindow configWindow;
    private final KeybindingsWindow keybindingsWindow;
    private final ImBoolean openBool = new ImBoolean(true);
    private final ImBoolean enabledScratch = new ImBoolean(false);
    private final ImBoolean enabledOnly = new ImBoolean(false);
    private final ImString search = new ImString(64);

    private Category selectedCategory = null;
    private Module selectedModule = null;

    public ModuleListWindow(ModuleConfigWindow configWindow,
                            KeybindingsWindow keybindingsWindow) {
        this.configWindow = configWindow;
        this.keybindingsWindow = keybindingsWindow;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(860, 520, ImGuiCond.Once);
        ImGui.begin("Korppu", openBool, ImGuiWindowFlags.None);

        renderToolbar();
        ImGui.separator();

        ImGui.beginChild("##categories", SIDEBAR_WIDTH, 0, true);
        renderCategories();
        ImGui.endChild();

        ImGui.sameLine();
        ImGui.beginChild("##modules", -DETAIL_WIDTH, 0, true);
        renderModuleTable();
        ImGui.endChild();

        ImGui.sameLine();
        ImGui.beginChild("##details", 0, 0, true);
        renderDetails();
        ImGui.endChild();

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
            configWindow.closeAll();
            keybindingsWindow.hide();
        }
    }

    private void renderToolbar() {
        if (ImGui.button("Keybindings")) {
            keybindingsWindow.toggle();
        }
        ImGui.sameLine();
        if (ImGui.button("Items")) {
            KorppuImGui.openItemRegistry();
        }
        ImGui.sameLine();
        if (ImGui.button("Profiles")) {
            KorppuImGui.openLayoutPresets();
        }
        ImGui.sameLine();
        ImGui.setNextItemWidth(220);
        ImGui.inputTextWithHint("##module_search", "Search modules...", search);
        ImGui.sameLine();
        ImGui.checkbox("Enabled only", enabledOnly);
    }

    private void renderCategories() {
        List<Module> all = ModuleManager.getModules();
        long allEnabled = all.stream().filter(Module::isEnabled).count();
        if (ImGui.selectable("All  " + allEnabled + "/" + all.size() + "##cat_all", selectedCategory == null)) {
            selectedCategory = null;
        }

        for (Category category : Category.values()) {
            List<Module> inCategory = all.stream().filter(m -> m.getCategory() == category).toList();
            if (inCategory.isEmpty()) continue;
            long enabled = inCategory.stream().filter(Module::isEnabled).count();
            String label = category.getDisplayName() + "  " + enabled + "/" + inCategory.size() + "##cat_" + category.name();
            if (ImGui.selectable(label, selectedCategory == category)) {
                selectedCategory = category;
            }
        }
    }

    private boolean matchesFilter(Module module) {
        if (enabledOnly.get() && !module.isEnabled()) return false;

        String query = search.get().trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            return module.getName().toLowerCase(Locale.ROOT).contains(query)
                    || module.getDescription().toLowerCase(Locale.ROOT).contains(query);
        }
        return selectedCategory == null || module.getCategory() == selectedCategory;
    }

    private void renderModuleTable() {
        int flags = ImGuiTableFlags.RowBg | ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.ScrollY;
        if (!ImGui.beginTable("##module_table", 4, flags)) return;

        ImGui.tableSetupColumn("##on", ImGuiTableColumnFlags.WidthFixed, 22);
        ImGui.tableSetupColumn("Module", ImGuiTableColumnFlags.WidthFixed, 150);
        ImGui.tableSetupColumn("Status", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##actions", ImGuiTableColumnFlags.WidthFixed, 22);
        ImGui.tableSetupScrollFreeze(0, 1);
        ImGui.tableHeadersRow();

        for (Module module : ModuleManager.getModules()) {
            if (!matchesFilter(module)) continue;

            ImGui.tableNextRow();

            ImGui.tableNextColumn();
            enabledScratch.set(module.isEnabled());
            if (ImGui.checkbox("##en_" + module.getName(), enabledScratch)) {
                module.setEnabled(enabledScratch.get());
            }

            ImGui.tableNextColumn();
            if (ImGui.selectable(module.getName() + "##sel_" + module.getName(), module == selectedModule,
                    ImGuiSelectableFlags.AllowDoubleClick)) {
                selectedModule = module;
                if (ImGui.isMouseDoubleClicked(0) && ModuleConfigWindow.hasVisibleSettings(module)) {
                    configWindow.openForModule(module);
                }
            }
            if (ImGui.isItemHovered() && !module.getDescription().isEmpty()) {
                ImGui.setTooltip(module.getDescription());
            }

            ImGui.tableNextColumn();
            String status = module.getStatus();
            if (!status.isEmpty()) {
                ImGui.textDisabled(status);
            }

            ImGui.tableNextColumn();
            if (hasStatusView(module)) {
                String statusButton = module.hasControlWindow() ? "C" : "S";
                if (ImGui.smallButton(statusButton + "##s_" + module.getName())) {
                    KorppuImGui.openStatusWindow(module);
                }
            }
        }

        ImGui.endTable();
    }

    private void renderDetails() {
        if (selectedModule == null) {
            ImGui.textDisabled("Select a module to see its settings.");
            ImGui.textDisabled("Double-click opens settings in a separate window.");
            return;
        }

        Module module = selectedModule;
        ImGui.text(module.getName());
        ImGui.sameLine();
        ImGui.textDisabled("(" + module.getCategory().getDisplayName() + ")");
        if (!module.getDescription().isEmpty()) {
            ImGui.textWrapped(module.getDescription());
        }

        ImGui.separator();

        enabledScratch.set(module.isEnabled());
        if (ImGui.checkbox("Enabled##detail_en", enabledScratch)) {
            module.setEnabled(enabledScratch.get());
        }
        ImGui.sameLine();
        String key = module.getKeybind().isUnbound() ? "none" : module.getKeybind().getTranslatedKeyMessage().getString();
        ImGui.textDisabled("Key: " + key);

        boolean hasSettings = ModuleConfigWindow.hasVisibleSettings(module);
        if (hasSettings && ImGui.button("Pop out##detail_popout")) {
            configWindow.openForModule(module);
        }
        if (hasStatusView(module)) {
            if (hasSettings) ImGui.sameLine();
            if (ImGui.button((module.hasControlWindow() ? "Control" : "Status") + "##detail_status")) {
                KorppuImGui.openStatusWindow(module);
            }
        }

        String status = module.getStatus();
        if (!status.isEmpty()) {
            ImGui.textDisabled(status);
        }

        ImGui.separator();

        if (!hasSettings) {
            ImGui.textDisabled("No settings.");
            return;
        }
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() * 0.55f);
        configWindow.renderSettings(module);
        ImGui.popItemWidth();
    }

    private static boolean hasStatusView(Module module) {
        return !module.getStatusEntries().isEmpty() || module.hasControlWindow();
    }
}
