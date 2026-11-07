package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.CommandManager;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.imgui.windows.InteractionMenuControlWindow;
import dev.mlml.korppu.imgui.windows.ModuleControlWindow;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRecord;
import dev.mlml.korppu.player.PlayerRelation;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class InteractionMenu extends Module {
    private final ListSetting<PrimaryAction> primaryAction = config.add(new ListSetting<>(
            "Primary Action", "Default context action", PrimaryAction.TOGGLE_RELATION));

    private final List<UUID> candidates = new ArrayList<>();
    private int selectedIndex;

    public InteractionMenu() {
        super("InteractionMenu", "Quick player actions menu", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        refreshCandidates();
    }

    @Override
    public void onTick() {
        refreshCandidates();
    }

    @Override
    public boolean hasControlWindow() {
        return true;
    }

    @Override
    public ModuleControlWindow createControlWindow() {
        return new InteractionMenuControlWindow(this);
    }

    public void cycleTarget(int delta) {
        if (candidates.isEmpty()) return;
        int size = candidates.size();
        selectedIndex = (selectedIndex + delta) % size;
        if (selectedIndex < 0) selectedIndex += size;
    }

    public void runPrimaryAction() {
        switch (primaryAction.getValue()) {
            case TOGGLE_RELATION -> toggleRelation();
            case TELEPORT -> teleportToTarget();
            case INVSEE -> invseeTarget();
        }
    }

    public void setFriend() {
        setRelation(PlayerRelation.FRIEND);
    }

    public void setEnemy() {
        setRelation(PlayerRelation.ENEMY);
    }

    public void setNeutral() {
        setRelation(PlayerRelation.NEUTRAL);
    }

    public void toggleRelation() {
        Player target = getSelectedPlayer();
        if (target == null) return;

        PlayerRelation current = PlayerManager.getInstance().getRelation(target.getUUID());
        PlayerRelation next = switch (current) {
            case NEUTRAL -> PlayerRelation.FRIEND;
            case FRIEND -> PlayerRelation.ENEMY;
            case ENEMY -> PlayerRelation.NEUTRAL;
        };
        setRelation(next);
    }

    public void teleportToTarget() {
        Player target = getSelectedPlayer();
        if (target == null) return;
        if (KorppuMod.mc.player == null) return;
        KorppuMod.mc.player.setPos(target.getX(), target.getY(), target.getZ());
    }

    public void invseeTarget() {
        Player target = getSelectedPlayer();
        if (target == null) return;
        CommandManager.execute("invsee " + target.getGameProfile().name());
    }

    public String getSelectedName() {
        Player target = getSelectedPlayer();
        return target == null ? "none" : target.getGameProfile().name();
    }

    public String getSelectedRelation() {
        Player target = getSelectedPlayer();
        if (target == null) return "N/A";
        return PlayerManager.getInstance().getRelation(target.getUUID()).name();
    }

    public int getCandidateCount() {
        return candidates.size();
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Target", getSelectedName()),
                StatusEntry.text("Relation", getSelectedRelation()),
                StatusEntry.text("Nearby", String.valueOf(getCandidateCount())),
                StatusEntry.text("Primary", primaryAction.getValue().name())
        );
    }

    private void setRelation(PlayerRelation relation) {
        Player target = getSelectedPlayer();
        if (target == null) return;

        PlayerManager pm = PlayerManager.getInstance();
        PlayerRecord record = pm.get(target.getUUID());
        if (record == null) {
            pm.add(target.getUUID(), target.getGameProfile().name(), relation);
        } else {
            pm.updateRelation(target.getUUID(), relation);
        }
        pm.save();
    }

    private Player getSelectedPlayer() {
        if (KorppuMod.mc.level == null || candidates.isEmpty()) return null;
        if (selectedIndex < 0 || selectedIndex >= candidates.size()) selectedIndex = 0;
        UUID id = candidates.get(selectedIndex);
        return KorppuMod.mc.level.players().stream()
                .filter(p -> p.getUUID().equals(id))
                .findFirst().orElse(null);
    }

    private void refreshCandidates() {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) {
            candidates.clear();
            selectedIndex = 0;
            return;
        }

        UUID selected = null;
        if (!candidates.isEmpty() && selectedIndex >= 0 && selectedIndex < candidates.size()) {
            selected = candidates.get(selectedIndex);
        }

        candidates.clear();
        KorppuMod.mc.level.players().stream()
                .filter(p -> !p.getUUID().equals(KorppuMod.mc.player.getUUID()))
                .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(KorppuMod.mc.player)))
                .forEach(p -> candidates.add(p.getUUID()));

        if (candidates.isEmpty()) {
            selectedIndex = 0;
            return;
        }

        if (selected != null) {
            int idx = candidates.indexOf(selected);
            if (idx >= 0) {
                selectedIndex = idx;
                return;
            }
        }

        if (selectedIndex >= candidates.size()) selectedIndex = candidates.size() - 1;
        if (selectedIndex < 0) selectedIndex = 0;
    }

    public enum PrimaryAction {
        TOGGLE_RELATION,
        TELEPORT,
        INVSEE
    }
}
