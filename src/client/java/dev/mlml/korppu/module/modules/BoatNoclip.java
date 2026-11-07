package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BoatNoclip extends Module {
    private final DoubleSetting horizontalSpeed = config.add(new DoubleSetting(
            "Horizontal Speed", "Horizontal flight speed", 10d, 0d, 50d, 1));
    private final DoubleSetting verticalSpeed = config.add(new DoubleSetting(
            "Vertical Speed", "Vertical flight speed", 6d, 0d, 20d, 1));
    private final BooleanSetting antiKick = config.add(new BooleanSetting(
            "Anti Kick", "Periodically descend slightly to prevent fly kick", true));
    private final BooleanSetting boatPhase = config.add(new BooleanSetting(
            "Boat Phase", "Apply periodic vehicle glitch pulses for phasing", false));
    private final DoubleSetting phasePulseTicks = config.add(new DoubleSetting(
            "Phase Pulse Ticks", "Ticks between Boat Phase pulses", 8d, 1d, 40d, 0)
            .showOnlyIf(() -> boatPhase.getValue()));

    private int antiKickTimer = 0;
    private int phaseTimer = 0;

    public BoatNoclip() {
        super("BoatNoclip", "Allows riding a boat through blocks (noClip). Horizontal speeds over 5-6 may rubberband.", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player != null && KorppuMod.mc.player.getVehicle() instanceof AbstractBoat boat) {
            boat.noPhysics = false;
        }
        antiKickTimer = 0;
        phaseTimer = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;
        if (!(KorppuMod.mc.player.getVehicle() instanceof AbstractBoat boat)) return;

        boat.noPhysics = true;

        if (antiKick.getValue()) {
            antiKickTimer++;
            if (antiKickTimer >= 40) {
                antiKickTimer = 0;
                if (KorppuMod.mc.getConnection() != null) {
                    KorppuMod.mc.getConnection().send(
                            new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(
                                    new net.minecraft.world.phys.Vec3(boat.getX(), boat.getY() - 0.05, boat.getZ()),
                                    boat.getYRot(), boat.getXRot(), false
                            )
                    );
                }
            }
        }

        if (boatPhase.getValue() && KorppuMod.mc.getConnection() != null) {
            phaseTimer++;
            if (phaseTimer >= phasePulseTicks.getValue().intValue()) {
                phaseTimer = 0;
                KorppuMod.mc.getConnection().send(
                        new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(
                                new net.minecraft.world.phys.Vec3(boat.getX(), boat.getY() - 0.12, boat.getZ()),
                                boat.getYRot(),
                                boat.getXRot(),
                                false
                        )
                );
            }
        }

        var player = KorppuMod.mc.player;
        double speed = horizontalSpeed.getValue() / 20.0;
        double vSpeed = verticalSpeed.getValue() / 20.0;

        float yaw = (float) Math.toRadians(player.getYRot());
        double moveX = 0, moveZ = 0, moveY = 0;

        var options = KorppuMod.mc.options;
        if (options.keyUp.isDown()) {
            moveX -= Math.sin(yaw) * speed;
            moveZ += Math.cos(yaw) * speed;
        }
        if (options.keyDown.isDown()) {
            moveX += Math.sin(yaw) * speed;
            moveZ -= Math.cos(yaw) * speed;
        }
        if (options.keyJump.isDown()) moveY += vSpeed;
        if (options.keySprint.isDown()) moveY -= vSpeed;

        boat.setDeltaMovement(moveX, moveY, moveZ);
        boat.setYRot(player.getYRot());
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        boolean inBoat = KorppuMod.mc.player != null
                && KorppuMod.mc.player.getVehicle() instanceof AbstractBoat;
        return List.of(
                StatusEntry.indicator("In Boat", inBoat),
                StatusEntry.indicator("Boat Phase", boatPhase.getValue()),
                StatusEntry.text("H Speed", String.format("%.1f", horizontalSpeed.getValue())),
                StatusEntry.text("V Speed", String.format("%.1f", verticalSpeed.getValue()))
        );
    }

    @Override
    public String getStatus() {
        if (KorppuMod.mc.player != null && KorppuMod.mc.player.getVehicle() instanceof AbstractBoat) {
            return "Active in boat";
        }
        return "Waiting for boat";
    }
}
