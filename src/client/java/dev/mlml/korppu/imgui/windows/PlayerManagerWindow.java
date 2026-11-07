package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRecord;
import dev.mlml.korppu.player.PlayerRelation;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerManagerWindow extends ImGuiWindow {
    private final ImBoolean openBool = new ImBoolean(true);

    // Add-player inputs
    private final ImString addUuidInput = new ImString(64);
    private final ImString addNameInput = new ImString(64);
    private final ImInt addRelationIndex = new ImInt(1); // NEUTRAL default
    private String addError = "";

    // Edit state
    private UUID editingUuid = null;
    private final ImString editNotesInput = new ImString(256);
    private String editNotesOriginal = "";

    // Reusable scratch for the per-row relation combo (read back same frame).
    private final ImInt relScratch = new ImInt(0);

    private static final String[] RELATION_LABELS = {"Friend", "Neutral", "Enemy"};
    private static final PlayerRelation[] RELATIONS = {
        PlayerRelation.FRIEND, PlayerRelation.NEUTRAL, PlayerRelation.ENEMY
    };

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(600, 450, ImGuiCond.Once);
        ImGui.begin("Player Manager##pmgr", openBool);

        renderAddRow();
        ImGui.separator();
        renderTable();

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
            PlayerManager.getInstance().save();
        }
    }

    private void renderAddRow() {
        ImGui.text("Add Player:");
        ImGui.sameLine();
        ImGui.setNextItemWidth(200);
        ImGui.inputText("UUID##pmgr_uuid", addUuidInput);
        ImGui.sameLine();
        ImGui.setNextItemWidth(120);
        ImGui.inputText("Name##pmgr_name", addNameInput);
        ImGui.sameLine();
        ImGui.setNextItemWidth(90);
        ImGui.combo("##pmgr_rel", addRelationIndex, RELATION_LABELS);
        ImGui.sameLine();
        if (ImGui.button("Add##pmgr_add")) {
            tryAddPlayer();
        }
        if (!addError.isEmpty()) {
            ImGui.textColored(1f, 0.3f, 0.3f, 1f, addError);
        }
    }

    private void tryAddPlayer() {
        String uuidStr = addUuidInput.get().trim();
        String name = addNameInput.get().trim();
        addError = "";

        if (uuidStr.isEmpty()) {
            addError = "UUID is required";
            return;
        }
        if (name.isEmpty()) {
            addError = "Name is required";
            return;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            addError = "Invalid UUID format";
            return;
        }

        PlayerManager.getInstance().add(uuid, name, RELATIONS[addRelationIndex.get()]);
        addUuidInput.set("");
        addNameInput.set("");
    }

    private void renderTable() {
        int flags = ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY
                | ImGuiTableFlags.Resizable;
        if (!ImGui.beginTable("##pmgr_table", 5, flags, 0, ImGui.getContentRegionAvailY())) return;

        ImGui.tableSetupColumn("Name");
        ImGui.tableSetupColumn("UUID");
        ImGui.tableSetupColumn("Relation");
        ImGui.tableSetupColumn("Notes");
        ImGui.tableSetupColumn("Actions");
        ImGui.tableHeadersRow();

        List<PlayerRecord> toRemove = new ArrayList<>();
        for (PlayerRecord r : PlayerManager.getInstance().getAll()) {
            ImGui.tableNextRow();

            ImGui.tableSetColumnIndex(0);
            ImGui.text(r.getLastKnownName());

            ImGui.tableSetColumnIndex(1);
            ImGui.textDisabled(r.getUuid().toString());

            ImGui.tableSetColumnIndex(2);
            relScratch.set(relationIndex(r.getRelation()));
            ImGui.setNextItemWidth(90);
            if (ImGui.combo("##rel_" + r.getUuid(), relScratch, RELATION_LABELS)) {
                PlayerManager.getInstance().updateRelation(r.getUuid(), RELATIONS[relScratch.get()]);
            }

            ImGui.tableSetColumnIndex(3);
            if (editingUuid != null && editingUuid.equals(r.getUuid())) {
                ImGui.setNextItemWidth(150);
                if (ImGui.inputText("##notes_" + r.getUuid(), editNotesInput)) {
                    PlayerManager.getInstance().updateNotes(r.getUuid(), editNotesInput.get());
                }
                ImGui.sameLine();
                if (ImGui.smallButton("Done##notes_done_" + r.getUuid())) {
                    editingUuid = null;
                }
                ImGui.sameLine();
                if (ImGui.smallButton("Cancel##notes_cancel_" + r.getUuid())) {
                    PlayerManager.getInstance().updateNotes(r.getUuid(), editNotesOriginal);
                    editingUuid = null;
                }
            } else {
                ImGui.text(r.getNotes().isEmpty() ? "-" : r.getNotes());
                ImGui.sameLine();
                if (ImGui.smallButton("Edit##notes_edit_" + r.getUuid())) {
                    editingUuid = r.getUuid();
                    editNotesOriginal = r.getNotes();
                    editNotesInput.set(r.getNotes());
                }
            }

            ImGui.tableSetColumnIndex(4);
            if (ImGui.smallButton("Remove##pmgr_rm_" + r.getUuid())) {
                toRemove.add(r);
            }
        }

        toRemove.forEach(r -> PlayerManager.getInstance().remove(r.getUuid()));

        ImGui.endTable();
    }

    private int relationIndex(PlayerRelation rel) {
        for (int i = 0; i < RELATIONS.length; i++) {
            if (RELATIONS[i] == rel) return i;
        }
        return 1;
    }
}
