package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.imgui.EntityInfoRenderer;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.modules.EntityInfo;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public class PlayerInfoWindow extends ImGuiWindow {
    private PlayerInfo entry;
    private final ImBoolean openBool = new ImBoolean(true);

    public void openForPlayer(PlayerInfo entry) {
        this.entry = entry;
        this.visible = true;
    }

    @Override
    public void render() {
        if (!visible || entry == null) return;

        String name = entry.getProfile().name();
        openBool.set(true);

        ImGui.setNextWindowSize(300, 360, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(300, 200, Float.MAX_VALUE, Float.MAX_VALUE);
        if (ImGui.begin("Player: " + name + "##pinfo_" + entry.getProfile().id(),
                openBool, ImGuiWindowFlags.None)) {

            if (EntityInfo.showIdentityEnabled()) {
                ImGui.text("Name: " + name);
                if (entry.getTeam() != null)
                    ImGui.text("Team: " + entry.getTeam().getDisplayName().getString());
                if (entry.getTabListDisplayName() != null)
                    ImGui.text("Display: " + entry.getTabListDisplayName().getString());
            }

            if (EntityInfo.showPlayerDetailsEnabled()) {
                ImGui.text("UUID: " + entry.getProfile().id());
                ImGui.text("Ping: " + entry.getLatency() + " ms");
                GameType gm = entry.getGameMode();
                if (gm != null) ImGui.text("Gamemode: " + gm.name());
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                mc.level.players().stream()
                        .filter(p -> p.getUUID().equals(entry.getProfile().id()))
                        .findFirst()
                        .ifPresent(player -> {
                            if (EntityInfo.showHealthEnabled()) {
                                ImGui.separator();
                                ImGui.text(String.format("Health: %.1f / %.1f", player.getHealth(), player.getMaxHealth()));
                                if (player.getAbsorptionAmount() > 0) {
                                    ImGui.text(String.format("Absorption: %.1f", player.getAbsorptionAmount()));
                                }
                            }

                            if (EntityInfo.showPositionEnabled()) {
                                ImGui.separator();
                                ImGui.text(String.format("Pos: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()));
                                net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
                                double hVel = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                                ImGui.text(String.format("Vel: %.3f, %.3f, %.3f  (H: %.3f)", vel.x, vel.y, vel.z, hVel));
                                ImGui.text(String.format("Camera  Yaw: %6.1f  Pitch: %5.1f", player.getYRot(), player.getXRot()));
                                if (Rotations.isEnabled() && player == mc.player) {
                                    ImGui.text(String.format("Server  Yaw: %6.1f  Pitch: %5.1f", Rotations.getClientYaw(), Rotations.getClientPitch()));
                                }
                            }

                            if (EntityInfo.showPotionEffectsEnabled()) {
                                EntityInfoRenderer.renderEffects(player);
                            }

                            if (EntityInfo.showEquipmentEnabled()) {
                                EntityInfoRenderer.renderEquipment(player, "pinfo_eq_" + player.getId());
                            }
                        });
            }
        }
        ImGui.end();

        if (!openBool.get()) visible = false;
    }
}
