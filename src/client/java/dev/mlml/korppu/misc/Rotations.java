package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.ModuleManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class Rotations {

    @Getter
    @Setter
    private static boolean enabled = false;
    @Getter
    private static float clientPitch;
    @Getter
    private static float clientYaw;
    private static long lastModificationTime = 0;
    private static Vec3 targetV3;

    @Getter
    private static Vec3 lastKnownServerPos = Vec3.ZERO;

    static {
        KorppuMod.eventManager.register(Rotations.class);
    }

    static void timeoutCheck() {
        if (System.currentTimeMillis() - lastModificationTime > 1000) {
            disable();
        } else {
            enable();
        }
    }

    static void tick() {
        timeoutCheck();
    }

    public static void lookAtV3(Vec3 target) {
        targetV3 = target;
        lastModificationTime = System.currentTimeMillis();
        update();
    }

    public static Vec3 relativeToAbsolute(Vec3 absRootPos, Vec2 rotation, Vec3 relative) {
        double xOffset = relative.x;
        double yOffset = relative.y;
        double zOffset = relative.z;
        float rot = 0.017453292F;
        float f = Mth.cos((rotation.y + 90.0F) * rot);
        float g = Mth.sin((rotation.y + 90.0F) * rot);
        float h = Mth.cos(-rotation.x * rot);
        float i = Mth.sin(-rotation.x * rot);
        float j = Mth.cos((-rotation.x + 90.0F) * rot);
        float k = Mth.sin((-rotation.x + 90.0F) * rot);
        Vec3 vec3d2 = new Vec3(f * h, i, g * h);
        Vec3 vec3d3 = new Vec3(f * j, k, g * j);
        Vec3 vec3d4 = vec3d2.cross(vec3d3).scale(-1.0);
        double d = vec3d2.x * zOffset + vec3d3.x * yOffset + vec3d4.x * xOffset;
        double e = vec3d2.y * zOffset + vec3d3.y * yOffset + vec3d4.y * xOffset;
        double l = vec3d2.z * zOffset + vec3d3.z * yOffset + vec3d4.z * xOffset;
        return new Vec3(absRootPos.x + d, absRootPos.y + e, absRootPos.z + l);
    }

    public static void lookAtPositionSmoothServerSide(Vec3 target, double laziness) {
        double delX = target.x - Objects.requireNonNull(KorppuMod.mc.player).getX();
        double delZ = target.z - KorppuMod.mc.player.getZ();
        double delY = target.y - (KorppuMod.mc.player.getY() + KorppuMod.mc.player.getEyeHeight(KorppuMod.mc.player.getPose()));

        double required = Math.toDegrees(Math.atan2(delZ, delX)) - 90, delta, add, speed;
        double sqrt1 = Math.sqrt(delX * delX + delZ * delZ);
        double degTan = -Math.toDegrees(Math.atan2(delY, sqrt1));

        delta = Mth.wrapDegrees(required - clientYaw);
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta) {
            add = delta;
        }
        setClientYaw(clientYaw + (float) add);

        required = degTan;
        delta = Mth.wrapDegrees(required - clientPitch);
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta) {
            add = delta;
        }
        setClientPitch(clientPitch + (float) add);
    }

    public static void lookAtPositionSmooth(float pitch, float yaw, float laziness) {
        double delta, add, speed;

        delta = Mth.wrapDegrees(yaw - KorppuMod.mc.player.getYRot());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta) {
            add = delta;
        }
        KorppuMod.mc.player.setYRot(KorppuMod.mc.player.getYRot() + (float) add);

        delta = Mth.wrapDegrees(pitch - KorppuMod.mc.player.getXRot());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta) {
            add = delta;
        }
        KorppuMod.mc.player.setXRot(KorppuMod.mc.player.getXRot() + (float) add);
    }

    public static void lookAtPositionSmooth(Vec3 target, float laziness) {
        double delX = target.x - Objects.requireNonNull(KorppuMod.mc.player).getX();
        double delZ = target.z - KorppuMod.mc.player.getZ();
        double delY = target.y - (KorppuMod.mc.player.getY() + KorppuMod.mc.player.getEyeHeight(KorppuMod.mc.player.getPose()));

        double required = Math.toDegrees(Math.atan2(delZ, delX)) - 90, delta, add, speed;
        double sqrt1 = Math.sqrt(delX * delX + delZ * delZ);
        double degTan = -Math.toDegrees(Math.atan2(delY, sqrt1));

        delta = Mth.wrapDegrees(required - KorppuMod.mc.player.getYRot());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta) {
            add = delta;
        }
        KorppuMod.mc.player.setYRot(KorppuMod.mc.player.getYRot() + (float) add);

        required = degTan;
        delta = Mth.wrapDegrees(required - KorppuMod.mc.player.getXRot());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta) {
            add = delta;
        }
        KorppuMod.mc.player.setXRot(KorppuMod.mc.player.getXRot() + (float) add);
    }

    public static Rotation getPitchYaw(Vec3 targetV3) {
        return getPitchYawFromOtherEntity(Objects.requireNonNull(KorppuMod.mc.player).getEyePosition(), targetV3);
    }

    public static Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180);
        float g = -yaw * ((float) Math.PI / 180);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    public static Rotation getPitchYawFromOtherEntity(Vec3 eyePos, Vec3 targetV3) {
        double vec = 57.2957763671875;
        Vec3 target = targetV3.subtract(eyePos);
        double square = Math.sqrt(target.x * target.x + target.z * target.z);
        float pitch = Mth.wrapDegrees((float) -(Mth.atan2(target.y, square) * vec));
        float yaw = Mth.wrapDegrees((float) (Mth.atan2(target.z, target.x) * vec) - 90.0F);
        return new Rotation(pitch, yaw);
    }

    public static void update() {
        tick();
        if (targetV3 != null) {
            Rotation py = getPitchYaw(targetV3);
            clientYaw = py.yaw();
            clientPitch = py.pitch();
        }
    }

    public static void setClientPitch(float clientPitch) {
        lastModificationTime = System.currentTimeMillis();
        Rotations.clientPitch = clientPitch;
    }

    public static void setClientYaw(float clientYaw) {
        lastModificationTime = System.currentTimeMillis();
        Rotations.clientYaw = clientYaw;
    }

    public static void setClientRotation(Rotation r) {
        lastModificationTime = System.currentTimeMillis();
        Rotations.clientYaw = r.yaw();
        Rotations.clientPitch = r.pitch();
    }

    public static void enable() {
        if (!enabled && KorppuMod.mc.player != null) {
            clientYaw = KorppuMod.mc.player.getYRot();
            clientPitch = KorppuMod.mc.player.getXRot();
        }
        enabled = true;
    }

    public static void disable() {
        enabled = false;
        targetV3 = null;
    }

    @Listener
    public static void onPacketSend(PacketEvent.Sent event) {
        if (!enabled || !ModuleManager.isSendPackets()) {
            return;
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
            clientYaw = packet.getYRot(clientYaw);
            clientPitch = packet.getXRot(clientPitch);
            if (!event.isCancelled()) {
                lastKnownServerPos = new Vec3(packet.getX(lastKnownServerPos.x), packet.getY(lastKnownServerPos.y), packet.getZ(lastKnownServerPos.z));
            }
        }
    }

    @Listener
    public static void onPacketReceive(PacketEvent.Received pe) {
        if (!enabled) {
            return;
        }

        if (pe.getPacket() instanceof ClientboundPlayerPositionPacket p) {
            clientYaw = p.change().yRot();
            clientPitch = p.change().xRot();
        }
    }
}
