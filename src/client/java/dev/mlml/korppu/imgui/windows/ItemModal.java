package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.item.ItemRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public class ItemModal extends ImGuiWindow {
    private static ItemModal instance;

    private ItemStack stack = ItemStack.EMPTY;
    private final ImBoolean openBool = new ImBoolean(true);
    private final ImString snbtBuf = new ImString(1 << 20); // 1 MiB
    private final ImString registryName = new ImString(256);
    private String statusMsg = "";
    private boolean parsedDirty = false;
    private ItemStack parsedStack = ItemStack.EMPTY;

    public ItemModal() {
        instance = this;
    }

    public static void openForStack(ItemStack stack) {
        if (instance == null) return;
        instance.loadStack(stack.copy());
        instance.show();
    }

    private void loadStack(ItemStack s) {
        this.stack = s;
        this.parsedStack = s.copy();
        String snbt = ItemRegistry.serialize(s);
        snbtBuf.set(snbt != null ? snbt : "");
        registryName.set(s.isEmpty() ? "" : s.getHoverName().getString());
        statusMsg = "";
        parsedDirty = false;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(500, 520, ImGuiCond.Once);
        String title = stack.isEmpty() ? "Item Inspector" : stack.getHoverName().getString();
        if (!ImGui.begin("Item: " + title + "##item_modal", openBool,
                ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.end();
            if (!openBool.get()) visible = false;
            return;
        }

        renderDetails();

        ImGui.separator();
        ImGui.textDisabled("SNBT / Component Data");

        float editH = 160f;
        if (ImGui.inputTextMultiline("##snbt_edit", snbtBuf, -1f, editH,
                ImGuiInputTextFlags.None)) {
            parsedDirty = true;
        }

        if (ImGui.button("Parse & Apply##snbt_parse")) {
            tryParse();
        }
        ImGui.sameLine();
        if (ImGui.button("Reset##snbt_reset")) {
            loadStack(stack);
        }
        ImGui.sameLine();
        if (ImGui.button("Open in Editor##snbt_edit_btn")) {
            openInEditor();
        }

        ImGui.separator();
        ImGui.textDisabled("Registry");
        ImGui.pushItemWidth(200f);
        ImGui.inputText("Name##reg_name", registryName);
        ImGui.popItemWidth();
        ImGui.sameLine();
        if (ImGui.button("Save to Registry##reg_save")) {
            saveToRegistry();
        }

        if (KorppuMod.mc.player != null && KorppuMod.mc.gameMode != null &&
                KorppuMod.mc.player.gameMode() == GameType.CREATIVE) {
            ImGui.sameLine();
            if (ImGui.button("Spawn##spawn")) {
                spawnItem();
            }
        }

        if (!statusMsg.isEmpty()) {
            ImGui.spacing();
            ImGui.textDisabled(statusMsg);
        }

        ImGui.end();
        if (!openBool.get()) visible = false;
    }

    private void renderDetails() {
        ItemStack display = parsedDirty ? parsedStack : stack;
        if (display.isEmpty()) {
            ImGui.textDisabled("(empty stack)");
            return;
        }

        ImGui.textWrapped("Name: " + display.getHoverName().getString());
        ImGui.textWrapped("ID: " + display.getItem().toString());
        ImGui.text("Count: " + display.getCount());

        if (display.isDamageableItem()) {
            int max = display.getMaxDamage();
            int dmg = display.getDamageValue();
            float remaining = max > 0 ? (float)(max - dmg) / max : 1f;
            ImGui.text(String.format("Durability: %d / %d (%.0f%%)", max - dmg, max, remaining * 100));
        }

        ItemEnchantments enchants = display.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchants.isEmpty()) {
            ImGui.textDisabled("Enchantments:");
            if (ImGui.beginTable("##item_modal_enchants", 2,
                    ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.RowBg)) {
                ImGui.tableSetupColumn("Enchantment", 0, 0.7f);
                ImGui.tableSetupColumn("Level", 0, 0.3f);
                ImGui.tableHeadersRow();
                enchants.entrySet().forEach(e -> {
                    ImGui.tableNextRow();
                    ImGui.tableSetColumnIndex(0);
                    ImGui.text(e.getKey().value().description().getString());
                    ImGui.tableSetColumnIndex(1);
                    ImGui.text(String.valueOf(e.getIntValue()));
                });
                ImGui.endTable();
            }
        }
    }

    private void tryParse() {
        String snbt = snbtBuf.get().trim();
        if (snbt.isEmpty()) {
            statusMsg = "Empty input";
            return;
        }

        ItemStack result = ItemRegistry.deserialize(snbt);
        if (result.isEmpty()) {
            result = ItemRegistry.parseComponentSyntax(snbt);
        }

        if (result.isEmpty()) {
            statusMsg = "Parse failed - check syntax";
        } else {
            parsedStack = result;
            parsedDirty = false;
            stack = result.copy();
            String refreshed = ItemRegistry.serialize(result);
            if (refreshed != null) snbtBuf.set(refreshed);
            statusMsg = "Parsed OK: " + result.getHoverName().getString();
        }
    }

    private void openInEditor() {
        dev.mlml.korppu.imgui.KorppuImGui.openEditorWithContent(snbtBuf.get());
    }

    private void saveToRegistry() {
        String name = registryName.get().trim();
        if (name.isEmpty()) {
            statusMsg = "Enter a name first";
            return;
        }
        ItemStack toSave = parsedDirty ? parsedStack : stack;
        if (toSave.isEmpty()) {
            statusMsg = "No valid item to save";
            return;
        }
        String snbt = ItemRegistry.serialize(toSave);
        if (snbt == null) {
            statusMsg = "Serialization failed";
            return;
        }
        ItemRegistry.add(new ItemRegistry.Entry(name, snbt));
        statusMsg = "Saved \"" + name + "\" to registry";
    }

    private void spawnItem() {
        if (KorppuMod.mc.player == null) return;
        ItemStack toSpawn = (parsedDirty ? parsedStack : stack).copy();
        if (toSpawn.isEmpty()) {
            statusMsg = "No item to spawn";
            return;
        }
        String spawnedName = toSpawn.getHoverName().getString();
        KorppuMod.mc.player.getInventory().add(toSpawn);
        statusMsg = "Spawned " + spawnedName;
    }
}
