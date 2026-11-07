package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public enum RotationUtils
{
    ;

    public static Vec3 getEyesPos()
    {
        LocalPlayer player = KorppuMod.mc.player;
        if (player == null) return Vec3.ZERO;

        float eyeHeight = player.getEyeHeight(player.getPose());
        return player.position().add(0, eyeHeight, 0);
    }

    public static Vec3 getClientLookVec(float partialTicks)
    {
        if (KorppuMod.mc.player == null) return Vec3.ZERO;

        float yaw = KorppuMod.mc.player.getYRot(partialTicks);
        float pitch = KorppuMod.mc.player.getXRot(partialTicks);
        return new Rotation(pitch, yaw).toLookVec();
    }

    public static Rotation getNeededRotations(Vec3 vec)
    {
        Vec3 eyes = getEyesPos();

        double diffX = vec.x - eyes.x;
        double diffZ = vec.z - eyes.z;
        double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;

        double diffY = vec.y - eyes.y;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double pitch = -Math.toDegrees(Math.atan2(diffY, diffXZ));

        return Rotation.wrapped((float)yaw, (float)pitch);
    }
}
