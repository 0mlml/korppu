package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MultiUse extends Module {
    private final DoubleSetting extraUses = config.add(new DoubleSetting(
            "Extra Uses",
            "Additional item-use calls per click",
            1d,
            1d,
            10d,
            0
    ));
    private final BooleanSetting onlyOnMiss = config.add(new BooleanSetting(
            "Only On Miss",
            "Only repeat use when crosshair is not targeting an interactable",
            false
    ));

    public MultiUse() {
        super("MultiUse", "Repeats item use call multiple times per click", GLFW.GLFW_KEY_UNKNOWN);
    }

    public int getExtraUses() {
        return extraUses.getValue().intValue();
    }

    public boolean isOnlyOnMiss() {
        return onlyOnMiss.getValue();
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Extra Uses", String.valueOf(getExtraUses())),
                StatusEntry.indicator("Only On Miss", isOnlyOnMiss())
        );
    }
}
