package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PlayerRespawnEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class GhostMode extends Module {
    private final BooleanSetting fullFood = config.add(new BooleanSetting("Full Food", "Keep client-side hunger full while in ghost mode", true));

    private boolean active;

    public GhostMode() {
        super("GhostMode", "Keep playing client-side after death", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        active = false;
    }

    @Override
    public void onTick() {
        if (!active || KorppuMod.mc.player == null) return;

        if (KorppuMod.mc.player.getHealth() < 1f) {
            KorppuMod.mc.player.setHealth(20f);
        }

        if (fullFood.getValue() && KorppuMod.mc.player.getFoodData().getFoodLevel() < 20) {
            KorppuMod.mc.player.getFoodData().setFoodLevel(20);
        }
    }

    public void markGhostActive() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    @Listener
    public void onRespawn(PlayerRespawnEvent event) {
        active = false;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Ghost Active", active),
                StatusEntry.indicator("Full Food", fullFood.getValue())
        );
    }

    @Override
    public String getStatus() {
        return active ? "active" : "idle";
    }
}
