package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.imgui.KorppuImGui;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PlayerListWindow extends ImGuiWindow {
    @Getter
    private final PlayerInfoWindow playerInfoWindow = new PlayerInfoWindow();
    private final ImBoolean openBool = new ImBoolean(true);
    private final ImBoolean persistent = new ImBoolean(false);

    public boolean isPersistent() {
        return persistent.get();
    }

    @Override
    public boolean isVisible() {
        return visible || playerInfoWindow.isVisible();
    }

    @Override
    public void hide() {
        if (!persistent.get()) {
            super.hide();
        }
    }

    @Override
    public void render() {
        if (visible) {
            renderPlayerList();
        }
        playerInfoWindow.render();
    }

    private void renderPlayerList() {
        openBool.set(true);
        ImGui.setNextWindowSize(300, 400, ImGuiCond.Once);
        ImGui.begin("Players##playerlist", openBool);

        ImGui.checkbox("Keep open##plistpersist", persistent);
        ImGui.separator();

        ClientPacketListener handler = Minecraft.getInstance().getConnection();
        if (handler != null) {
            List<PlayerInfo> players = new ArrayList<>(handler.getListedOnlinePlayers());
            players.sort(Comparator.comparingInt(PlayerInfo::getLatency));

            for (PlayerInfo e : players) {
                String name = e.getProfile().name();
                int ping = e.getLatency();
                UUID uuid = e.getProfile().id();
                String label = String.format("%s  [%d ms]##p_%s", name, ping, uuid);

                if (ImGui.selectable(label)) {
                    playerInfoWindow.openForPlayer(e);
                }
                ImGui.sameLine();

                boolean alreadyTracked = KorppuImGui.getEntityListWindow().isTracked(uuid);
                if (alreadyTracked) {
                    ImGui.textDisabled("+");
                } else {
                    if (ImGui.button("+##track_" + uuid)) {
                        Minecraft mc = Minecraft.getInstance();
                        boolean found = false;
                        if (mc.level != null) {
                            for (Player p : mc.level.players()) {
                                if (p.getUUID().equals(uuid)) {
                                    KorppuImGui.getEntityListWindow().trackEntity(p);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            KorppuImGui.getEntityListWindow().trackByUuid(uuid, name);
                        }
                    }
                }
            }
        } else {
            ImGui.textDisabled("Not connected");
        }

        ImGui.end();

        if (!openBool.get()) {
            persistent.set(false);
            visible = false;
        }
    }
}
