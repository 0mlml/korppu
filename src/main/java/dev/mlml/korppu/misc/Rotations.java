package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.PacketEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Rotations
{

    @Getter
    @Setter
    private static boolean enabled = false;
    @Getter private static float clientPitch;
    @Getter private static float clientYaw;
    private static long lastModificationTime = 0;
    private static Vec3d targetV3;

    @Getter
    private static Vec3d lastKnownServerPos = Vec3d.ZERO;

    static
    {
        class RotationsPacketHandler
        {
            public void onPacketSend(PacketEvent.Sent event)
            {
                if (event.getPacket() instanceof PlayerMoveC2SPacket packet)
                {
                    clientYaw = packet.getYaw(clientYaw);
                    clientPitch = packet.getPitch(clientPitch);
                    if (!event.isCancelled())
                    {
                        lastKnownServerPos = new Vec3d(packet.getX(lastKnownServerPos.x), packet.getY(lastKnownServerPos.y), packet.getZ(lastKnownServerPos.z));
                    }
                }
            }

            public void onPacketReceive(PacketEvent.Received pe)
            {
                if (pe.getPacket() instanceof PlayerPositionLookS2CPacket p)
                {
                    clientYaw = p.getYaw();
                    clientPitch = p.getPitch();
                }
            }
        }

        KorppuMod.eventManager.register(new RotationsPacketHandler());
    }

    static void timeoutCheck()
    {
        if (System.currentTimeMillis() - lastModificationTime > 1000)
        {
            disable();
        } else
        {
            enable();
        }
    }

    static void tick()
    {
        timeoutCheck();
    }

    public static void lookAtV3(Vec3d target)
    {
        targetV3 = target;
        lastModificationTime = System.currentTimeMillis();
        update();
    }

    public static Vec3d relativeToAbsolute(Vec3d absRootPos, Vec2f rotation, Vec3d relative)
    {
        double xOffset = relative.x;
        double yOffset = relative.y;
        double zOffset = relative.z;
        float rot = 0.017453292F;
        float f = MathHelper.cos((rotation.y + 90.0F) * rot);
        float g = MathHelper.sin((rotation.y + 90.0F) * rot);
        float h = MathHelper.cos(-rotation.x * rot);
        float i = MathHelper.sin(-rotation.x * rot);
        float j = MathHelper.cos((-rotation.x + 90.0F) * rot);
        float k = MathHelper.sin((-rotation.x + 90.0F) * rot);
        Vec3d vec3d2 = new Vec3d(f * h, i, g * h);
        Vec3d vec3d3 = new Vec3d(f * j, k, g * j);
        Vec3d vec3d4 = vec3d2.crossProduct(vec3d3).multiply(-1.0D);
        double d = vec3d2.x * zOffset + vec3d3.x * yOffset + vec3d4.x * xOffset;
        double e = vec3d2.y * zOffset + vec3d3.y * yOffset + vec3d4.y * xOffset;
        double l = vec3d2.z * zOffset + vec3d3.z * yOffset + vec3d4.z * xOffset;
        return new Vec3d(absRootPos.x + d, absRootPos.y + e, absRootPos.z + l);
    }

    public static void lookAtPositionSmoothServerSide(Vec3d target, double laziness)
    {
        double delX = target.x - Objects.requireNonNull(KorppuMod.mc.player).getX();
        double delZ = target.z - KorppuMod.mc.player.getZ();
        double delY = target.y - (KorppuMod.mc.player.getY() + KorppuMod.mc.player.getEyeHeight(KorppuMod.mc.player.getPose()));

        double required = Math.toDegrees(Math.atan2(delZ, delX)) - 90, delta, add, speed;
        double sqrt1 = Math.sqrt(delX * delX + delZ * delZ);
        double degTan = -Math.toDegrees(Math.atan2(delY, sqrt1));

        // setting yaw
        delta = MathHelper.wrapDegrees(required - clientYaw);
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta)
        {
            add = delta;
        }
        setClientYaw(clientYaw + (float) add);

        // setting pitch
        required = degTan;
        delta = MathHelper.wrapDegrees(required - clientPitch);
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta)
        {
            add = delta;
        }
        setClientPitch(clientPitch + (float) add);
    }

    public static void lookAtPositionSmooth(float pitch, float yaw, float laziness)
    {
        double delta, add, speed;

        delta = MathHelper.wrapDegrees(yaw - KorppuMod.mc.player.getYaw());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta)
        {
            add = delta;
        }
        KorppuMod.mc.player.setYaw(KorppuMod.mc.player.getYaw() + (float) add);

        // setting pitch
        delta = MathHelper.wrapDegrees(pitch - KorppuMod.mc.player.getPitch());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta)
        {
            add = delta;
        }
        KorppuMod.mc.player.setPitch(KorppuMod.mc.player.getPitch() + (float) add);
    }

    public static void lookAtPositionSmooth(Vec3d target, float laziness)
    {
        double delX = target.x - Objects.requireNonNull(KorppuMod.mc.player).getX();
        double delZ = target.z - KorppuMod.mc.player.getZ();
        double delY = target.y - (KorppuMod.mc.player.getY() + KorppuMod.mc.player.getEyeHeight(KorppuMod.mc.player.getPose()));

        double required = Math.toDegrees(Math.atan2(delZ, delX)) - 90, delta, add, speed;
        double sqrt1 = Math.sqrt(delX * delX + delZ * delZ);
        double degTan = -Math.toDegrees(Math.atan2(delY, sqrt1));
        // setting yaw
        delta = MathHelper.wrapDegrees(required - KorppuMod.mc.player.getYaw());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta)
        {
            add = delta;
        }
        KorppuMod.mc.player.setYaw(KorppuMod.mc.player.getYaw() + (float) add);

        // setting pitch
        required = degTan;
        delta = MathHelper.wrapDegrees(required - KorppuMod.mc.player.getPitch());
        speed = Math.abs(delta / laziness);
        add = speed * (delta >= 0 ? 1 : -1);
        if (add >= 0 && add > delta || add < 0 && add < delta)
        {
            add = delta;
        }
        KorppuMod.mc.player.setPitch(KorppuMod.mc.player.getPitch() + (float) add);
    }

    public static Rotation getPitchYaw(Vec3d targetV3)
    {
        return getPitchYawFromOtherEntity(Objects.requireNonNull(KorppuMod.mc.player).getEyePos(), targetV3);
    }

    public static Vec3d getRotationVector(float pitch, float yaw)
    {
        float f = pitch * ((float) Math.PI / 180);
        float g = -yaw * ((float) Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public static Rotation getPitchYawFromOtherEntity(Vec3d eyePos, Vec3d targetV3)
    {
        double vec = 57.2957763671875;
        Vec3d target = targetV3.subtract(eyePos);
        double square = Math.sqrt(target.x * target.x + target.z * target.z);
        float pitch = MathHelper.wrapDegrees((float) -(MathHelper.atan2(target.y, square) * vec));
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(target.z, target.x) * vec) - 90.0F);
        return new Rotation(pitch, yaw);
    }

    public static void update()
    {
        tick();
        if (targetV3 != null)
        {
            Rotation py = getPitchYaw(targetV3);
            clientYaw = py.getYaw();
            clientPitch = py.getPitch();
        }
    }

    public static void setClientPitch(float clientPitch)
    {
        lastModificationTime = System.currentTimeMillis();
        Rotations.clientPitch = clientPitch;
    }

    public static void setClientYaw(float clientYaw)
    {
        lastModificationTime = System.currentTimeMillis();
        Rotations.clientYaw = clientYaw;
    }

    public static void setClientRotation(Rotation r)
    {
        lastModificationTime = System.currentTimeMillis();
        Rotations.clientYaw = r.getYaw();
        Rotations.clientPitch = r.getPitch();
    }

    public static void enable()
    {
        enabled = true;
    }

    public static void disable()
    {
        enabled = false;
        targetV3 = null;
    }
}
