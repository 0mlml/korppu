package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class InvDupe extends Module {
    private final DoubleSetting actionsPerTick = config.add(new DoubleSetting(
            "Actions/Tick", "Inventory actions sent per tick", 6d, 1d, 100d, 0));
    private final BooleanSetting hotbarOnly = config.add(new BooleanSetting(
            "Hotbar Only", "Only attempt actions in hotbar slots", true));

    private int sentActions;

    public InvDupe() {
        super("InvDupe", "Spam inventory quick-move/throw actions", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;

        int count = actionsPerTick.getValue().intValue();
        int start = hotbarOnly.getValue() ? 0 : 0;
        int end = hotbarOnly.getValue() ? 9 : 36;

        var handler = KorppuMod.mc.player.containerMenu;
        for (int i = 0; i < count; i++) {
            int invSlot = start + (i % Math.max(1, (end - start)));
            int screenSlot = invSlot < 9 ? invSlot + 36 : invSlot;

            KorppuMod.mc.gameMode.handleContainerInput(
                    handler.containerId,
                    screenSlot,
                    0,
                    ContainerInput.QUICK_MOVE,
                    KorppuMod.mc.player
            );
            KorppuMod.mc.gameMode.handleContainerInput(
                    handler.containerId,
                    screenSlot,
                    1,
                    ContainerInput.THROW,
                    KorppuMod.mc.player
            );
            sentActions += 2;
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Actions", String.valueOf(sentActions)),
                StatusEntry.text("Per Tick", String.valueOf(actionsPerTick.getValue().intValue())),
                StatusEntry.indicator("Hotbar Only", hotbarOnly.getValue())
        );
    }
}
