package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.imgui.EntityInfoRenderer;
import dev.mlml.korppu.imgui.HpTracker;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.imgui.TrackedEntity;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntityDetailWindow extends ImGuiWindow {
    private final ImBoolean openBool = new ImBoolean(true);

    private Entity crosshairEntity = null;
    private final HpTracker crosshairTracker = new HpTracker();

    private TrackedEntity pinnedEntity = null;

    public void setCrosshairEntity(Entity entity) {
        if (entity != crosshairEntity) {
            crosshairTracker.reset();
        }
        crosshairEntity = entity;
        if (entity instanceof LivingEntity le) {
            crosshairTracker.update(le);
        }
    }

    public void pinEntity(TrackedEntity te) {
        this.pinnedEntity = te;
        this.visible = true;
    }

    @Override
    public void render() {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        openBool.set(true);
        ImGui.setNextWindowSize(300, 320, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(300, 200, Float.MAX_VALUE, Float.MAX_VALUE);

        TrackedEntity pinned = pinnedEntity;
        String title = pinned != null
                ? "Entity: " + pinned.getDisplayName() + "##entitydetail"
                : "Target Info##entitydetail";

        if (ImGui.begin(title, openBool, ImGuiWindowFlags.None)) {
            if (pinned != null) {
                if (ImGui.button("Unpin")) {
                    pinnedEntity = null;
                }
                ImGui.separator();
                Entity live = pinned.liveEntity;
                if (live != null && !live.isRemoved()) {
                    EntityInfoRenderer.renderEntity(mc, live, pinned.hpTracker);
                } else {
                    ImGui.textDisabled(pinned.lastKnownDead ? "(dead, showing snapshot)" : "(out of render distance, showing snapshot)");
                    EntityInfoRenderer.renderSnapshot(pinned);
                }
            } else {
                if (crosshairEntity == null) {
                    ImGui.textDisabled("No entity under crosshair");
                } else {
                    boolean alreadyTracked = KorppuImGui.getEntityListWindow().isTracked(crosshairEntity.getUUID());
                    if (!alreadyTracked) {
                        if (ImGui.button("Track")) {
                            KorppuImGui.getEntityListWindow().trackEntity(crosshairEntity);
                        }
                        ImGui.separator();
                    }
                    EntityInfoRenderer.renderEntity(mc, crosshairEntity, crosshairTracker);
                }
            }
        }
        ImGui.end();

        if (!openBool.get()) {
            visible = false;
            pinnedEntity = null;
        }
    }
}
