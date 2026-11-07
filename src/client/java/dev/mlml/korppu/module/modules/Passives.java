package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.GenericSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.ILivingEntityMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class Passives extends Module {

    @Getter
    private final BooleanSetting noBreakDelay = config.add(new BooleanSetting("No Break Delay", "Break blocks faster", false));
    @Getter
    private final BooleanSetting noPlaceDelay = config.add(new BooleanSetting("No Place Delay", "Place blocks faster", false));
    private final BooleanSetting noJumpDelay = config.add(new BooleanSetting("No Jump Delay", "Jump faster", false));
    @Getter
    private final BooleanSetting noLevitation = config.add(new BooleanSetting("No Levitation", "No levitation effect", false));
    @Getter
    private final BooleanSetting noBlind = config.add(new BooleanSetting("No Blind", "No blindness effect", false));
    @Getter
    private final BooleanSetting sendPackets = config.add(new BooleanSetting("Send Packets", "Kill switch for modifying sent packets", true));
    @Getter
    private final BooleanSetting infiniteChatLength = config.add(new BooleanSetting("Infinite chat length", "Unrestrict length of chat", true));
    @Getter
    private final BooleanSetting nightVision = config.add(new BooleanSetting("Nightvision", "Give nv",
            true));
    @Getter
    private final BooleanSetting portalGUI = config.add(new BooleanSetting("Portal GUI", "Allow portal GUI", true));
    @Getter
    private final BooleanSetting noSlow = config.add(new BooleanSetting("No Slow", "Don't slow for item use", false));
    @Getter
    private final BooleanSetting renderStructureVoid = config.add(new BooleanSetting("Render Structure Void", "Render structure void blocks", false));
    @Getter
    private final BooleanSetting dinnerboneEntities = config.add(new BooleanSetting("Dinnerbone Entities", "Render entities upside-down", false));
    @Getter
    private final BooleanSetting deadmau5Ears = config.add(new BooleanSetting("Deadmau5 Ears", "Show deadmau5 ears on players", false));
    @Getter
    private final BooleanSetting christmasChests = config.add(new BooleanSetting("Christmas Chests", "Force festive chest textures", false));
    @Getter
    private final ListSetting<ShaderMode> shaderMode = config.add(new ListSetting<>("Shader", "Apply vanilla post-processing shader", ShaderMode.None));

    public Passives() {
        super("Passive", "Passive tweaks", GLFW.GLFW_KEY_BACKSLASH);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("No Break Delay", noBreakDelay.getValue()),
                StatusEntry.indicator("No Place Delay", noPlaceDelay.getValue()),
                StatusEntry.indicator("No Jump Delay", noJumpDelay.getValue()),
                StatusEntry.indicator("No Levitation", noLevitation.getValue()),
                StatusEntry.indicator("No Blind", noBlind.getValue()),
                StatusEntry.indicator("No Slow", noSlow.getValue()),
                StatusEntry.indicator("Night Vision", nightVision.getValue()),
                StatusEntry.indicator("Portal GUI", portalGUI.getValue()),
                StatusEntry.indicator("Structure Void", renderStructureVoid.getValue()),
                StatusEntry.indicator("Dinnerbone", dinnerboneEntities.getValue()),
                StatusEntry.indicator("Deadmau5 Ears", deadmau5Ears.getValue()),
                StatusEntry.indicator("Christmas Chests", christmasChests.getValue()),
                StatusEntry.text("Shader", shaderMode.getValue().name()),
                StatusEntry.indicator("Infinite Chat", infiniteChatLength.getValue()),
                StatusEntry.indicator("Send Packets", sendPackets.getValue()));
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
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

    public enum ShaderMode {
        None("none"),
        Blur("blur"),
        Creeper("creeper"),
        Invert("invert"),
        Spider("spider"),
        Antialias("antialias"),
        Bits("bits"),
        Blob("blob"),
        Deconverge("deconverge"),
        Desaturate("desaturate"),
        Flip("flip"),
        Green("green"),
        Ntsc("ntsc"),
        Outline("outline"),
        Pencil("pencil"),
        Phosphor("phosphor"),
        Sobel("sobel"),
        Wobble("wobble");

        private final String shaderId;

        ShaderMode(String shaderId) {
            this.shaderId = shaderId;
        }

        public String shaderId() {
            return shaderId;
        }
    }
}
