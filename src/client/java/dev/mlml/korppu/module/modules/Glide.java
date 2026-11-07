package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class Glide extends Module {
    private final DoubleSetting fallSpeed = config.add(new DoubleSetting("Fall Speed", "Maximum downward speed while gliding", 0.08d, 0.01d, 1d, 2));

    public Glide() {
        super("Glide", "Slows falling with a configurable downward speed", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null) return;
        if (KorppuMod.mc.player.onGround() || KorppuMod.mc.player.isInWater()) return;

        double maxDownward = -Math.abs(fallSpeed.getValue());
        if (KorppuMod.mc.player.getDeltaMovement().y < maxDownward) {
            KorppuMod.mc.player.setDeltaMovement(
                    KorppuMod.mc.player.getDeltaMovement().x,
                    maxDownward,
                    KorppuMod.mc.player.getDeltaMovement().z
            );
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(StatusEntry.text("Fall Speed", String.format("%.2f", fallSpeed.getValue())));
    }
}
