package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.GenericSetting;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.ILivingEntityMixin;
import dev.mlml.korppu.module.Module;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class Passives extends Module {

    @Getter
    private final BooleanSetting reach = config.add(new BooleanSetting("Reach", "Reach further", false));
    @Getter
    private final DoubleSetting blockReachDistance = config.add(new DoubleSetting("Block Reach Distance", "Reach distance", 6.0, 1.0, 10.0, 1));
    @Getter
    private final DoubleSetting entityReachDistance = config.add(new DoubleSetting("Entity Reach Distance", "Entity reach distance", 6.0, 1.0, 10.0, 1));
    private final BooleanSetting fullBright = config.add(new BooleanSetting("Full Bright", "Full bright", false));
    @Getter
    private final BooleanSetting noBreakDelay = config.add(new BooleanSetting("No Break Delay", "Break blocks faster", false));
    @Getter
    private final BooleanSetting noPlaceDelay = config.add(new BooleanSetting("No Place Delay", "Place blocks faster", false));
    private final BooleanSetting noJumpDelay = config.add(new BooleanSetting("No Jump Delay", "Jump faster", false));
    @Getter
    private final BooleanSetting noLevitation = config.add(new BooleanSetting("No Levitation", "No levitation effect", false));
    @Getter
    private final BooleanSetting moreChatHistory = config.add(new BooleanSetting("More Chat History", "More chat history", true));

    public Passives() {
        super("Passive", "Passive tweaks", -1);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null) {
            return;
        }

        if (noJumpDelay.getValue()) {
            ((ILivingEntityMixin) KorppuMod.mc.player).setJumpingCooldown(0);
        }
    }

    @Override
    public String getStatus() {
        int count = 0;
        int enabled = 0;

        for (GenericSetting<?> setting : config.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
                count++;
                if (booleanSetting.getValue()) {
                    enabled++;
                }
            }
        }

        return TextFormatter.format("%1%s | %2%s", TextFormatter.Code.GREEN, enabled, TextFormatter.Code.RED, count - enabled);
    }
}
