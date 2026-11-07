package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

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
    public List<StatusEntry> getStatusEntries() {
        return switch (mode.getValue()) {
            case Vanilla -> List.of(
                    StatusEntry.text("Mode", "Vanilla"),
                    StatusEntry.text("Speed", String.format("%.1f", vanillaSpeed.getValue())));
            case BHop -> List.of(
                    StatusEntry.text("Mode", "BHop"),
                    StatusEntry.text("Distance", String.format("%.1f", bhopDistance.getValue())));
            case Legit -> List.of(StatusEntry.text("Mode", "Legit"));
        };
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
        Vec3 movementVec = new Vec3(0, 0, 0);

        if (KorppuMod.mc.options.keyUp.isDown()) {
            movementVec = movementVec.add(Vec3.directionFromRotation(0, KorppuMod.mc.player.getYRot()));
        }
        if (KorppuMod.mc.options.keyDown.isDown()) {
            movementVec = movementVec.add(Vec3.directionFromRotation(0, KorppuMod.mc.player.getYRot() - 180));
        }
        if (KorppuMod.mc.options.keyLeft.isDown()) {
            movementVec = movementVec.add(Vec3.directionFromRotation(0, KorppuMod.mc.player.getYRot() - 90));
        }
        if (KorppuMod.mc.options.keyRight.isDown()) {
            movementVec = movementVec.add(Vec3.directionFromRotation(0, KorppuMod.mc.player.getYRot() + 90));
        }

        if (movementVec.lengthSqr() > 0) {
            movementVec = movementVec.normalize().scale(bhopDistance.getValue() / 2);
        } else {
            return;
        }

        if (!KorppuMod.mc.player.onGround()) {
            if (KorppuMod.mc.player.getDeltaMovement().y < 0) {
                KorppuMod.mc.player.setDeltaMovement(KorppuMod.mc.player.getDeltaMovement().multiply(1, 1.1, 1));
            }
            return;
        }

        Vec3 hop = new Vec3(movementVec.x, 0.42, movementVec.z);
        KorppuMod.mc.player.setDeltaMovement(hop);
    }

    private void doLegit() {
        if (!KorppuMod.mc.options.keyUp.isDown() || KorppuMod.mc.options.keyDown.isDown()) {
            return;
        }

        if (KorppuMod.mc.player.isCrouching() || KorppuMod.mc.player.onClimbable() || !KorppuMod.mc.player.onGround() || KorppuMod.mc.player.horizontalCollision) {
            return;
        }

        KorppuMod.mc.player.setSprinting(true);
        KorppuMod.mc.player.jumpFromGround();
    }

    public enum Mode {
        Vanilla, BHop, Legit,
    }
}