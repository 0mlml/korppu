package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class AntiAim extends Module {
    private final ListSetting<Mode> yawMode = config.add(new ListSetting<>("Yaw Mode", "How to handle yaw", Mode.Static));
    private final ListSetting<Pitch> pitchMode = config.add(new ListSetting<>("Pitch Mode", "How to handle pitch", Pitch.Static));
    private final DoubleSetting yawOffset = config.add(new DoubleSetting("Yaw", "Yaw value for static", 0d, -180d, 180d, 0));
    private final DoubleSetting pitchOffset = config.add(new DoubleSetting("Pitch", "Pitch value for static", 0d, -90d, 90d, 0));

    public AntiAim() {
        super("AntiAim", "Hides your head rotation in silly ways", GLFW.GLFW_KEY_SEMICOLON);
    }

    private Perspective previousPerspective = Perspective.FIRST_PERSON;
    private float previousYaw = 0;
    private float previousPitch = 0;

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        previousPerspective = KorppuMod.mc.options.getPerspective();
        previousYaw = KorppuMod.mc.player.getYaw();
        previousPitch = KorppuMod.mc.player.getPitch();
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        KorppuMod.mc.options.setPerspective(previousPerspective);

        KorppuMod.mc.player.setYaw(previousYaw);
        KorppuMod.mc.player.setPitch(previousPitch);
    }

    @Override
    public void onTick() {
        float desiredYaw;

        switch (yawMode.getValue()) {
            case Spin:
                desiredYaw = (float) (System.currentTimeMillis() % 360);
                break;
            case Jitter:
                if (System.currentTimeMillis() % 1000 < 500) {
                    desiredYaw = 180;
                } else {
                    desiredYaw = -180;
                }
                break;
            default:
                desiredYaw = 0;
                break;
        }
        desiredYaw = MathHelper.wrapDegrees(desiredYaw + yawOffset.getValue().floatValue());
        Rotations.setClientYaw(desiredYaw);

        float desiredPitch;
        float lastPitch = Rotations.getClientPitch();
        desiredPitch = switch (pitchMode.getValue()) {
            case Decrease -> lastPitch - 5;
            case Increase -> lastPitch + 5;
            case Random -> (float) (Math.random() * 180 - 90);
            default -> 0;
        };
        desiredPitch = MathHelper.wrapDegrees(desiredPitch + pitchOffset.getValue().floatValue());
        Rotations.setClientPitch(desiredPitch);

        if (KorppuMod.mc.player == null) {
            return;
        }

        Objects.requireNonNull(KorppuMod.mc.getNetworkHandler()).sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(desiredYaw, desiredPitch, Objects.requireNonNull(KorppuMod.mc.player).isOnGround()));
    }

    @Override
    public void onWorldRender(WorldRenderContext wrc) {
        KorppuMod.mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public enum Mode {
        Spin, Jitter, Random, Static;
    }

    public enum Pitch {
        Decrease, Increase, Random, Static;
    }
}
