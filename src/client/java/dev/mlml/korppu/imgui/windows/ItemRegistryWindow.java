package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.item.ItemRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

public class ItemRegistryWindow extends ImGuiWindow {
    private final ImBoolean openBool = new ImBoolean(true);
    private final ImString searchBuf = new ImString(128);
    private int selectedIndex = -1;

    @Override
    public WindowType getWindowType() { return WindowType.MENU; }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(480, 520, ImGuiCond.Once);
        if (!ImGui.begin("Item Registry##korppu_reg", openBool, ImGuiWindowFlags.None)) {
            ImGui.end();
            if (!openBool.get()) visible = false;
            return;
        }

        if (ImGui.button("New Item##reg_new")) {
            ItemModal.openForStack(ItemStack.EMPTY);
        }
        ImGui.sameLine();
        if (ImGui.button("Presets##reg_presets")) {
            KorppuImGui.openItemPresets();
        }
        ImGui.sameLine();
        if (ImGui.button("Reload##reg_reload")) {
            ItemRegistry.load();
            selectedIndex = -1;
        }

        ImGui.pushItemWidth(-1);
        ImGui.inputText("##reg_search (filter)", searchBuf);
        ImGui.popItemWidth();

        ImGui.separator();

        List<ItemRegistry.Entry> all = ItemRegistry.getEntries();
        String filter = searchBuf.get().toLowerCase();

        if (ImGui.beginTable("##reg_table", 3,
                ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.RowBg |
                ImGuiTableFlags.ScrollY | ImGuiTableFlags.BordersInnerH,
                0, ImGui.getContentRegionAvailY() - 30)) {

            ImGui.tableSetupScrollFreeze(0, 1);
            ImGui.tableSetupColumn("Name", 0, 0.5f);
            ImGui.tableSetupColumn("Preview", 0, 0.35f);
            ImGui.tableSetupColumn("Actions", 0, 0.15f);
            ImGui.tableHeadersRow();

            int shown = 0;
            for (int i = 0; i < all.size(); i++) {
                ItemRegistry.Entry entry = all.get(i);
                if (!filter.isEmpty() && !entry.name().toLowerCase().contains(filter)) continue;
                shown++;

                ImGui.tableNextRow();

                ImGui.tableSetColumnIndex(0);
                boolean selected = selectedIndex == i;
                if (ImGui.selectable(entry.name() + "##reg_row_" + i, selected)) {
                    selectedIndex = i;
                }

                ImGui.tableSetColumnIndex(1);
                ItemStack preview = ItemRegistry.deserialize(entry.snbt());
                if (!preview.isEmpty()) {
                    ImGui.textDisabled(preview.getHoverName().getString());
                } else {
                    ImGui.textDisabled("(parse error)");
                }

                ImGui.tableSetColumnIndex(2);
                renderRowActions(i, entry, preview);
            }

            if (shown == 0) {
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.textDisabled(all.isEmpty() ? "(registry is empty)" : "(no items match filter)");
            }

            ImGui.endTable();
        }

        ImGui.textDisabled(all.size() + " item(s) in registry");

        ImGui.end();
        if (!openBool.get()) visible = false;
    }

    private void renderRowActions(int index, ItemRegistry.Entry entry, ItemStack preview) {
        if (ImGui.smallButton("E##reg_e_" + index)) {
            if (!preview.isEmpty()) {
                ItemModal.openForStack(preview);
            } else {
                ItemStack empty = ItemStack.EMPTY;
                ItemModal.openForStack(empty);
                KorppuImGui.openEditorWithContent(entry.snbt());
            }
        }
        ImGui.sameLine();
        if (ImGui.smallButton("X##reg_d_" + index)) {
            ItemRegistry.remove(index);
            if (selectedIndex >= ItemRegistry.getEntries().size()) selectedIndex = -1;
        }
        if (KorppuMod.mc.player != null && KorppuMod.mc.gameMode != null &&
                KorppuMod.mc.player.gameMode() == GameType.CREATIVE) {
            ImGui.sameLine();
            if (ImGui.smallButton("S##reg_s_" + index)) {
                if (!preview.isEmpty()) {
                    KorppuMod.mc.player.getInventory().add(preview.copy());
                }
            }
        }
    }
}
