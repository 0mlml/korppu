package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;

import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class EntityFly extends Module {
    private DoubleSetting verticalSpeed = config.add(new DoubleSetting("Vertical Speed", "Vertical fly speed", 9d, 0d, 50d, 1));
    private DoubleSetting horizontalSpeed = config.add(new DoubleSetting("Horizontal Speed", "Horizontal fly speed", 5d, 0d, 20d, 1));
    private DoubleSetting fallSpeed = config.add(new DoubleSetting("Fall Speed", "Speed when falling", 0.1d, 0d, 20d, 1));
    private BooleanSetting dropServerPacket = config.add(new BooleanSetting("Drop Server Packet", "Drop incoming server packets", false));
    private KeyMapping down = new KeyMapping("key.korppumod." + getName().replaceAll(" ", "")
            .toLowerCase() + "_down", GLFW.GLFW_KEY_LEFT_ALT, kbCategory);

    Entity lastVehicle = null;

    public EntityFly() {
        super("EntityFly", "Allows you to fly with entities.", GLFW.GLFW_KEY_UNKNOWN);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }
        Entity vehicle = KorppuMod.mc.player.getVehicle();
        if (vehicle == null) {
            return;
        }
        lastVehicle = vehicle;
        vehicle.setNoGravity(true);
        if (vehicle instanceof Mob) {
            ((Mob) vehicle).setNoAi(true);
        }
        Vec3 nv3 = getVec3d();
        vehicle.setDeltaMovement(nv3);
        vehicle.setYRot(KorppuMod.mc.player.getYRot());
        ServerboundMoveVehiclePacket p = ServerboundMoveVehiclePacket.fromEntity(vehicle);
        Objects.requireNonNull(KorppuMod.mc.getConnection()).send(p);
    }

    private @NotNull Vec3 getVec3d() {
        Options go = KorppuMod.mc.options;
        float y = Objects.requireNonNull(KorppuMod.mc.player).getYRot();
        int mx = 0, my = 0, mz = 0;
        if (go.keyJump.isDown()) {
            my++;
        }
        if (go.keyDown.isDown()) {
            mz++;
        }
        if (go.keyLeft.isDown()) {
            mx--;
        }
        if (go.keyRight.isDown()) {
            mx++;
        }
        if (down.isDown()) {
            my--;
        }
        if (go.keyUp.isDown()) {
            mz--;
        }
        double sin = Math.sin(Math.toRadians(y));
        double cos = Math.cos(Math.toRadians(y));
        double nx = horizontalSpeed.getValue() * mz * sin;
        double nz = horizontalSpeed.getValue() * mz * -cos;
        double ny = verticalSpeed.getValue() * my;
        nx += mx * -cos;
        nz += mx * -sin;
        Vec3 nv3 = new Vec3(nx, ny - fallSpeed.getValue(), nz);
        return nv3;
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }
        if (lastVehicle != null) {
            lastVehicle.setNoGravity(false);
            if (lastVehicle instanceof Mob) {
                ((Mob) lastVehicle).setNoAi(false);
            }
        }
    }

    @Listener
    public void onPacketSend(@NotNull PacketEvent.Sent event) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }
        if (dropServerPacket.getValue() && event.getPacket() instanceof ServerboundMoveVehiclePacket) {
            event.setCancelled(true);
        }
    }
}
