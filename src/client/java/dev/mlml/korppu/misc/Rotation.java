package dev.mlml.korppu.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@AllArgsConstructor
@Setter
@Getter
public class Rotation implements Cloneable {
    private float pitch, yaw;

    public Rotation normalize() {
        this.pitch = Mth.wrapDegrees(this.pitch);
        this.yaw = Mth.wrapDegrees(this.yaw);
        return this;
    }

    /**
     * Creates a Rotation with wrapped angles
     */
    public static Rotation wrapped(float yaw, float pitch) {
        return new Rotation(
            Mth.wrapDegrees(pitch),
            Mth.wrapDegrees(yaw)
        );
    }

    /**
     * Converts rotation to look vector
     */
    public Vec3 toLookVec() {
        float f = pitch * ((float) Math.PI / 180);
        float g = -yaw * ((float) Math.PI / 180);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    /**
     * Returns pitch as a record accessor (for compatibility)
     */
    public float pitch() {
        return pitch;
    }

    /**
     * Returns yaw as a record accessor (for compatibility)
     */
    public float yaw() {
        return yaw;
    }

    /**
     * Calculates angle between this rotation and another
     */
    public double getAngleTo(Rotation other) {
        float yawDiff = Mth.wrapDegrees(other.yaw - this.yaw);
        float pitchDiff = other.pitch - this.pitch;

        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    @Override
    public Rotation clone() {
        try {
            return (Rotation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}