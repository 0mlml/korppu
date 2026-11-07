package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.imgui.EntityInfoRenderer;
import dev.mlml.korppu.imgui.TrackedEntity;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class EntityListWindow extends ImGuiWindow {
    private final ImBoolean openBool = new ImBoolean(true);
    private final ImBoolean showHp = new ImBoolean(true);
    private final ImBoolean showDist = new ImBoolean(true);
    private final ImBoolean showType = new ImBoolean(false);

    @Getter
    private final List<TrackedEntity> tracked = new ArrayList<>();
    private final EntityDetailWindow detailWindow;
    private final ImString idInput = new ImString(16);

    public EntityListWindow(EntityDetailWindow detailWindow) {
        this.detailWindow = detailWindow;
    }

    public void trackEntity(Entity entity) {
        UUID uuid = entity.getUUID();
        for (TrackedEntity te : tracked) {
            if (te.uuid.equals(uuid)) {
                te.updateFrom(entity);
                return;
            }
        }
        tracked.add(new TrackedEntity(entity));
    }

    public void trackByUuid(UUID uuid, String name) {
        for (TrackedEntity te : tracked) {
            if (te.uuid.equals(uuid)) return;
        }
        tracked.add(new TrackedEntity(uuid, name));
    }

    public boolean isTracked(UUID uuid) {
        for (TrackedEntity te : tracked) {
            if (te.uuid.equals(uuid)) return true;
        }
        return false;
    }

    public TrackedEntity getTracked(UUID uuid) {
        for (TrackedEntity te : tracked) {
            if (te.uuid.equals(uuid)) return te;
        }
        return null;
    }

    public TrackedEntity removeTracked(UUID uuid) {
        for (int i = 0; i < tracked.size(); i++) {
            TrackedEntity te = tracked.get(i);
            if (!te.uuid.equals(uuid)) continue;
            tracked.remove(i);
            return te;
        }
        return null;
    }

    private void tryAddById(String input) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            int id = Integer.parseInt(input);
            Entity entity = mc.level.getEntity(id);
            if (entity != null) {
                trackEntity(entity);
            }
        } catch (NumberFormatException ignored) {}
    }

    public void tick(Minecraft mc) {
        if (mc.level == null) {
            for (TrackedEntity te : tracked) te.liveEntity = null;
            return;
        }
        Map<UUID, Entity> worldMap = new HashMap<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            worldMap.put(e.getUUID(), e);
        }
        for (TrackedEntity te : tracked) {
            Entity live = worldMap.get(te.uuid);
            if (live != null && !live.isRemoved()) {
                te.updateFrom(live);
            } else {
                te.liveEntity = null;
            }
        }
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(320, 260, ImGuiCond.Once);

        if (ImGui.begin("Entity List##entitylist", openBool)) {
            ImGui.checkbox("HP##col_hp", showHp);
            ImGui.sameLine();
            ImGui.checkbox("Dist##col_dist", showDist);
            ImGui.sameLine();
            ImGui.checkbox("Type##col_type", showType);
            ImGui.separator();

            if (tracked.isEmpty()) {
                ImGui.textDisabled("No tracked entities. Use + in player list or track from crosshair.");
            } else {
                renderTable();
            }

            ImGui.separator();
            ImGui.setNextItemWidth(-60);
            boolean enter = ImGui.inputText("##entid", idInput, ImGuiInputTextFlags.EnterReturnsTrue);
            ImGui.sameLine();
            if ((ImGui.button("Add##entid_add") || enter) && !idInput.get().isBlank()) {
                tryAddById(idInput.get().trim());
                idInput.set("");
            }
            if (ImGui.isItemHovered() || ImGui.isItemActive()) {
                ImGui.beginTooltip();
                ImGui.text("Enter entity ID");
                ImGui.endTooltip();
            }
        }
        ImGui.end();

        if (!openBool.get()) visible = false;
    }

    private void renderTable() {
        int numCols = 2;
        if (showHp.get()) numCols++;
        if (showDist.get()) numCols++;
        if (showType.get()) numCols++;

        int flags = ImGuiTableFlags.RowBg | ImGuiTableFlags.BordersOuter
                | ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.ScrollY;
        ImGui.setNextWindowContentSize(0, 0);
        if (!ImGui.beginTable("##tracked_ents", numCols, flags, -1f, -1f)) return;

        ImGui.tableSetupScrollFreeze(0, 1);
        ImGui.tableSetupColumn("Name", 0, 0.35f);
        if (showType.get()) ImGui.tableSetupColumn("Type", 0, 0.2f);
        if (showHp.get())   ImGui.tableSetupColumn("HP",   0, 0.25f);
        if (showDist.get()) ImGui.tableSetupColumn("Dist", 0, 0.15f);
        ImGui.tableSetupColumn("-", 0, 0.05f);
        ImGui.tableHeadersRow();

        List<TrackedEntity> toRemove = new ArrayList<>();
        for (int i = 0; i < tracked.size(); i++) {
            TrackedEntity te = tracked.get(i);
            boolean online = te.isOnline();

            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(0);

            if (!online) ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.55f, 0.55f, 0.55f, 1f);
            boolean clicked = ImGui.selectable(
                    te.getDisplayName() + "##te_row_" + i,
                    false,
                    ImGuiSelectableFlags.None);
            if (!online) ImGui.popStyleColor();
            if (clicked) detailWindow.pinEntity(te);

            int col = 1;
            if (showType.get()) {
                ImGui.tableSetColumnIndex(col++);
                ImGui.textDisabled(te.lastKnownType != null ? te.lastKnownType : "?");
            }
            if (showHp.get()) {
                ImGui.tableSetColumnIndex(col++);
                if (te.lastKnownHp >= 0 && te.lastKnownMaxHp > 0) {
                    float frac = te.lastKnownHp / te.lastKnownMaxHp;
                    int c = frac > 0.6f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFAA00 : 0xFFFF5555;
                    EntityInfoRenderer.pushArgbColor(c);
                    ImGui.progressBar(frac, -1f, 10f, String.format("%.0f", te.lastKnownHp));
                    ImGui.popStyleColor();
                } else {
                    ImGui.textDisabled("-");
                }
            }
            if (showDist.get()) {
                ImGui.tableSetColumnIndex(col++);
                Minecraft mc = Minecraft.getInstance();
                if (online && mc.player != null) {
                    ImGui.text(String.format("%.0fm", mc.player.distanceTo(te.liveEntity)));
                } else {
                    ImGui.textDisabled("?");
                }
            }
            ImGui.tableSetColumnIndex(col);
            if (ImGui.button("-##rm_" + i)) {
                toRemove.add(te);
            }
        }

        ImGui.endTable();
        tracked.removeAll(toRemove);
    }
}
