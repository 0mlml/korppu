package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.EventManager;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.Module;
import net.minecraft.client.CameraType;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class AntiAim extends Module {
    private final ListSetting<Mode> yawMode = config.add(new ListSetting<>("Yaw Mode", "How to handle yaw", Mode.Static));
    private final ListSetting<Pitch> pitchMode = config.add(new ListSetting<>("Pitch Mode", "How to handle pitch", Pitch.Static));
    private final DoubleSetting yawOffset = config.add(new DoubleSetting("Yaw", "Yaw offset added on top of mode value", 180d, -180d, 180d, 0));
    private final DoubleSetting pitchOffset = config.add(new DoubleSetting("Pitch", "Pitch value for static mode", 90d, -90d, 90d, 0));
    private final DoubleSetting spinSpeed = config.add(new DoubleSetting("Spin Speed", "Degrees per second for Spin mode", 180d, 10d, 1440d, 0));
    private final BooleanSetting forceThirdPerson = config.add(new BooleanSetting("Force Thirdperson", "Go thirdperson when " +
            "active", true));
    private final BooleanSetting blockSwings = config.add(new BooleanSetting("Block swing animation", "Don't send arm swing " +
            "packets", true));

    public AntiAim() {
        super("AntiAim", "Hides your head rotation in silly ways", GLFW.GLFW_KEY_SEMICOLON);

        KorppuMod.eventManager.register(this);
    }

    private CameraType previousPerspective = CameraType.FIRST_PERSON;

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null) return;
        previousPerspective = KorppuMod.mc.options.getCameraType();
        Rotations.enable();
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player == null) return;
        KorppuMod.mc.options.setCameraType(previousPerspective);
        Rotations.disable();
    }

    @Override
    public void onTick() {
        Rotations.enable();

        float desiredYaw = switch (yawMode.getValue()) {
            case Spin -> (float) ((System.currentTimeMillis() * spinSpeed.getValue() / 1000.0) % 360);
            case Jitter -> (System.currentTimeMillis() % 200 < 100) ? 180f : -180f;
            case Random -> (float) (Math.random() * 360 - 180);
            default -> 0f;
        };
        desiredYaw = Mth.wrapDegrees(desiredYaw + yawOffset.getValue().floatValue());
        Rotations.setClientYaw(desiredYaw);

        float lastPitch = Rotations.getClientPitch();
        float desiredPitch = switch (pitchMode.getValue()) {
            case Decrease -> Mth.wrapDegrees(lastPitch - 5f) / 4;
            case Increase -> Mth.wrapDegrees(lastPitch + 5f) / 4;
            case Random -> (float) (Math.random() * 180 - 90);
            default -> 0f;
        };
        if (pitchMode.getValue() == Pitch.Static) {
            desiredPitch = Mth.clamp(pitchOffset.getValue().floatValue(), -90f, 90f);
        } else {
            desiredPitch = Mth.clamp(desiredPitch + pitchOffset.getValue().floatValue(), -90f, 90f);
        }
        Rotations.setClientPitch(desiredPitch);

        if (forceThirdPerson.getValue()) {
            KorppuMod.mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    @Listener
    public void onPacket(PacketEvent.Sent event) {
        if (isEnabled() && event.getPacket() instanceof ServerboundSwingPacket) {
            event.cancel();
        }
    }

    public enum Mode {
        Spin, Jitter, Random, Static;
    }

    public enum Pitch {
        Decrease, Increase, Random, Static;
    }
}
