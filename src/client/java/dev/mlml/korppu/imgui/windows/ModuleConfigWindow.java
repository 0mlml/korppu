package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.config.*;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleConfigWindow extends ImGuiWindow {
    @Override
    public WindowType getWindowType() { return WindowType.MENU; }

    public void closeAll() {
        moduleVisible.replaceAll((k, v) -> false);
    }

    private final ConcurrentHashMap<String, Boolean> moduleVisible = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImBoolean> moduleOpenBools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImString> searchBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImString> customBuffers = new ConcurrentHashMap<>();

    // Reusable scratch widgets: each setting renders one widget at a time and the
    // value is read back the same frame, so a single shared instance per type is safe.
    private final ImBoolean boolScratch = new ImBoolean(false);
    private final float[] floatScratch = {0f};
    private final ImString stringScratch = new ImString(256);
    private final ImInt intScratch = new ImInt(0);

    public boolean isOpenFor(Module module) {
        return Boolean.TRUE.equals(moduleVisible.get(module.getName()));
    }

    public void openForModule(Module module) {
        moduleVisible.put(module.getName(), true);
    }

    @Override
    public boolean isVisible() {
        return moduleVisible.containsValue(Boolean.TRUE);
    }

    @Override
    public void render() {
        for (Module module : ModuleManager.getModules()) {
            if (!Boolean.TRUE.equals(moduleVisible.get(module.getName()))) continue;

            ImBoolean openBool = moduleOpenBools.computeIfAbsent(module.getName(), k -> new ImBoolean(true));
            openBool.set(true);

            ImGui.setNextWindowSize(300, 0, ImGuiCond.Once);
            if (ImGui.begin(module.getName() + " Settings##cfg_" + module.getName(), openBool,
                    ImGuiWindowFlags.AlwaysAutoResize)) {
                renderSettings(module);
            }
            ImGui.end();

            if (!openBool.get()) {
                moduleVisible.put(module.getName(), false);
            }
        }
    }

    public static boolean hasVisibleSettings(Module module) {
        return module.getConfig().getSettings().stream().anyMatch(s -> !(s instanceof HiddenSetting));
    }

    public void renderSettings(Module module) {
        for (GenericSetting<?> setting : module.getConfig().getSettings()) {
            renderSetting(setting);
        }
    }

    @SuppressWarnings({"rawtypes"})
    private void renderSetting(GenericSetting<?> setting) {
        if (setting instanceof HiddenSetting) return;
        if (!setting.isVisible()) return;

        if (setting instanceof BooleanSetting b) {
            boolScratch.set(b.getValue());
            if (ImGui.checkbox(setting.getName(), boolScratch)) {
                b.setValue(boolScratch.get());
            }
        } else if (setting instanceof DoubleSetting d) {
            floatScratch[0] = d.getValue().floatValue();
            String fmt = "%." + d.getPrecision() + "f";
            if (ImGui.sliderFloat(setting.getName(), floatScratch,
                    d.getMin().floatValue(), d.getMax().floatValue(), fmt)) {
                d.setValue((double) floatScratch[0]);
            }
        } else if (setting instanceof StringSetting s) {
            stringScratch.set(s.getValue());
            if (ImGui.inputText(setting.getName(), stringScratch)) {
                s.setValue(stringScratch.get());
            }
        } else if (setting instanceof ListSetting ls) {
            Enum<?>[] possible = ls.getPossible();
            String[] options = Arrays.stream(possible).map(Enum::name).toArray(String[]::new);
            int currentIdx = Arrays.asList(possible).indexOf(ls.getValue());
            intScratch.set(Math.max(0, currentIdx));
            if (ImGui.combo(setting.getName(), intScratch, options)) {
                ls.setValueFromString(options[intScratch.get()]);
            }
        } else if (setting instanceof MultiSelectSetting ms) {
            renderMultiSelectSetting(ms);
            return;
        }

        if (!setting.getTooltip().isEmpty() && ImGui.isItemHovered()) {
            ImGui.setTooltip(setting.getTooltip());
        }
    }

    private void renderMultiSelectSetting(MultiSelectSetting ms) {
        String key = ms.getName();
        List<String> allOptions = ms.getOptions();

        ImGui.text(ms.getName() + " (" + ms.getValue().size() + " selected):");
        if (!ms.getTooltip().isEmpty() && ImGui.isItemHovered()) {
            ImGui.setTooltip(ms.getTooltip());
        }

        String filter = "";
        if (ms.isSearchable()) {
            ImString searchBuf = searchBuffers.computeIfAbsent(key, k -> new ImString(128));
            ImGui.setNextItemWidth(-1);
            ImGui.inputTextWithHint("##search_" + key, "Filter...", searchBuf);
            filter = searchBuf.get().toLowerCase(Locale.ROOT);
        }

        ImGui.beginChild("##msl_" + key, 0, 200, true);
        final String f = filter;

        for (String opt : allOptions) {
            if (!ms.isSelected(opt)) continue;
            if (!f.isEmpty() && !opt.toLowerCase(Locale.ROOT).contains(f)) continue;
            boolScratch.set(true);
            if (ImGui.checkbox(opt + "##msl_" + key, boolScratch)) ms.toggle(opt);
        }

        for (String opt : allOptions) {
            if (ms.isSelected(opt)) continue;
            if (!f.isEmpty() && !opt.toLowerCase(Locale.ROOT).contains(f)) continue;
            boolScratch.set(false);
            if (ImGui.checkbox(opt + "##msl_" + key, boolScratch)) ms.toggle(opt);
        }

        ImGui.endChild();

        if (ms.isAllowCustom()) {
            ImString customBuf = customBuffers.computeIfAbsent(key, k -> new ImString(128));
            ImGui.setNextItemWidth(-60);
            ImGui.inputTextWithHint("##custom_" + key, "Custom entry...", customBuf);
            ImGui.sameLine();
            if (ImGui.button("Add##" + key)) {
                String entry = customBuf.get().trim();
                if (!entry.isEmpty() && !ms.isSelected(entry)) {
                    ms.toggle(entry);
                    customBuf.set("");
                }
            }
        }
    }
}
