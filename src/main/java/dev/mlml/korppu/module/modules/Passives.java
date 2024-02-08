package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.GenericSetting;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.ILivingEntityMixin;
import dev.mlml.korppu.module.Module;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class Passives extends Module {

    private final BooleanSetting reach = config.add(new BooleanSetting("Reach", "Reach further", false));
    private final BooleanSetting noBreakDelay = config.add(new BooleanSetting("No Break Delay", "Break blocks faster", false));
    private final BooleanSetting noPlaceDelay = config.add(new BooleanSetting("No Place Delay", "Place blocks faster", false));
    private final BooleanSetting noJumpDelay = config.add(new BooleanSetting("No Jump Delay", "Jump faster", false));
    @Getter
    private final BooleanSetting noLevitation = config.add(new BooleanSetting("No Levitation", "No levitation effect", false));
    @Getter
    private final BooleanSetting sendPackets = config.add(new BooleanSetting("Send Packets", "Kill switch for modifying sent packets", true));
    @Getter
    private final BooleanSetting moreChatHistory = config.add(new BooleanSetting("More Chat History", "More chat history", true));

    public Passives() {
        super("Passive", "Passive tweaks", ModuleType.WORLD, GLFW.GLFW_KEY_BACKSLASH);
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
