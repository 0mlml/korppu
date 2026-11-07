package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class InvDupeModule extends Module {
    private final DoubleSetting actionsPerTick = config.add(new DoubleSetting(
            "Actions/Tick", "Inventory actions sent per tick", 10d, 1d, 200d, 0));

    private int sent;

    public InvDupeModule() {
        super("InvDupeModule", "Legacy inventory dupe action spam", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;

        int count = actionsPerTick.getValue().intValue();
        var handler = KorppuMod.mc.player.containerMenu;
        for (int i = 0; i < count; i++) {
            int slot = 36 + (i % 9);
            KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, slot, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
            KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, slot, 1, ContainerInput.THROW, KorppuMod.mc.player);
            sent += 2;
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Sent", String.valueOf(sent)),
                StatusEntry.text("Per Tick", String.valueOf(actionsPerTick.getValue().intValue()))
        );
    }
}
