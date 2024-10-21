package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.module.Module;
import lombok.Getter;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Speed extends Module {
    @Getter
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Speed mode", Mode.Vanilla));
    @Getter
    private final DoubleSetting vanillaSpeed = config.add(new DoubleSetting("Vanilla Speed", "Speed in vanilla mode", 2.0, 0.0, 20.0, 1));
    private final DoubleSetting bhopDistance = config.add(new DoubleSetting("BHop Distance", "Distance to jump", 2.0, 0.0, 20.0, 1));

    public Speed() {
        super("Speed", "Go fast", GLFW.GLFW_KEY_C);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        switch (mode.getValue()) {
            case BHop -> doBhop();
            case Legit -> doLegit();
        }
    }

    @Override
    public String getStatus() {
        switch (mode.getValue()) {
            case Vanilla -> {
                return "SPD: " + vanillaSpeed.getValue();
            }
            case BHop -> {
                return "DST: " + bhopDistance.getValue();
            }
            case Legit -> {
                return "Legit";
            }
        }
        return "";
    }

    private void doBhop() {
        Vec3d movementVec = new Vec3d(0, 0, 0);

        if (KorppuMod.mc.options.forwardKey.isPressed()) {
            movementVec = movementVec.add(Vec3d.fromPolar(0, KorppuMod.mc.player.getYaw()));
        }
        if (KorppuMod.mc.options.backKey.isPressed()) {
            movementVec = movementVec.add(Vec3d.fromPolar(0, KorppuMod.mc.player.getYaw() - 180));
        }
        if (KorppuMod.mc.options.leftKey.isPressed()) {
            movementVec = movementVec.add(Vec3d.fromPolar(0, KorppuMod.mc.player.getYaw() - 90));
        }
        if (KorppuMod.mc.options.rightKey.isPressed()) {
            movementVec = movementVec.add(Vec3d.fromPolar(0, KorppuMod.mc.player.getYaw() + 90));
        }

        if (movementVec.lengthSquared() > 0) {
            movementVec = movementVec.normalize().multiply(bhopDistance.getValue() / 2);
        } else {
            return;
        }

        if (!KorppuMod.mc.player.isOnGround()) {
            if (KorppuMod.mc.player.getVelocity().y < 0) {
                KorppuMod.mc.player.setVelocity(KorppuMod.mc.player.getVelocity().multiply(1, 1.1, 1));
            }
            return;
        }

        Vec3d hop = new Vec3d(movementVec.x, 0.42, movementVec.z);
        KorppuMod.mc.player.setVelocity(hop);
    }

    private void doLegit() {
        if (!KorppuMod.mc.options.forwardKey.isPressed() || KorppuMod.mc.options.backKey.isPressed()) {
            return;
        }

        if (KorppuMod.mc.player.isSneaking() || KorppuMod.mc.player.isClimbing() || !KorppuMod.mc.player.isOnGround() || KorppuMod.mc.player.horizontalCollision) {
            return;
        }

        KorppuMod.mc.player.setSprinting(true);
        KorppuMod.mc.player.jump();
    }

    public enum Mode {
        Vanilla, BHop, Legit,
    }
}
