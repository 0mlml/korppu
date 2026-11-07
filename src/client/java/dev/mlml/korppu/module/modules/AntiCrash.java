package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.GenericSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

public class AntiCrash extends Module {
    private final BooleanSetting guardExplosions = config.add(new BooleanSetting("Guard Explosions", "Cancel malformed explosion packets", true));
    private final BooleanSetting guardParticles = config.add(new BooleanSetting("Guard Particles", "Cancel malformed particle packets", true));
    private final BooleanSetting guardPositionLook = config.add(new BooleanSetting("Guard Position Look", "Cancel invalid position look packets", true));
    private final BooleanSetting guardEntityVelocity = config.add(new BooleanSetting("Guard Entity Velocity", "Cancel extreme entity velocity packets", true));
    private final DoubleSetting maxAbsoluteCoordinate = config.add(new DoubleSetting("Max Abs Coordinate", "Maximum allowed absolute coordinate", 30000000d, 1000d, 30000000d, 0));
    private final DoubleSetting maxAbsoluteVelocity = config.add(new DoubleSetting("Max Abs Velocity", "Maximum allowed absolute velocity", 1000d, 1d, 100000d, 0));
    private final DoubleSetting maxParticleValue = config.add(new DoubleSetting("Max Particle Value", "Maximum allowed particle coordinate/speed", 1000000d, 1d, 100000000d, 0));

    private int blockedPackets;

    public AntiCrash() {
        super("AntiCrash", "Cancels malformed packets that can freeze/crash clients", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Explosions", guardExplosions.getValue()),
                StatusEntry.indicator("Particles", guardParticles.getValue()),
                StatusEntry.indicator("Position Look", guardPositionLook.getValue()),
                StatusEntry.indicator("Velocity", guardEntityVelocity.getValue()),
                StatusEntry.text("Blocked", String.valueOf(blockedPackets))
        );
    }

    @Override
    public String getStatus() {
        return blockedPackets + " blocked";
    }

    @Listener
    public void onPacket(PacketEvent.Received event) {
        if (!isEnabled() || event.getPacket() == null) return;

        String packetName = event.getPacket().getClass().getSimpleName();
        boolean cancelled = false;

        if (guardExplosions.getValue() && packetName.contains("Explosion")) {
            cancelled = hasInvalidCoordinates(event.getPacket(), maxAbsoluteCoordinate.getValue());
        } else if (guardParticles.getValue() && packetName.contains("Particle")) {
            cancelled = hasInvalidCoordinates(event.getPacket(), maxParticleValue.getValue());
        } else if (guardPositionLook.getValue() && packetName.contains("PositionLook")) {
            cancelled = hasInvalidCoordinates(event.getPacket(), maxAbsoluteCoordinate.getValue());
        } else if (guardEntityVelocity.getValue() && event.getPacket() instanceof ClientboundSetEntityMotionPacket velPacket) {
            double limit = maxAbsoluteVelocity.getValue();
            var vel = velPacket.movement();
            cancelled = Math.abs(vel.x) > limit || Math.abs(vel.y) > limit || Math.abs(vel.z) > limit;
        }

        if (cancelled) {
            blockedPackets++;
            event.cancel();
        }
    }

    private static boolean hasInvalidCoordinates(Object packet, double limit) {
        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> type = field.getType();

                if (type == double.class) {
                    double value = field.getDouble(packet);
                    if (!Double.isFinite(value)) return true;
                    if (Math.abs(value) > limit) return true;
                } else if (type == float.class) {
                    float value = field.getFloat(packet);
                    if (!Float.isFinite(value)) return true;
                    if (Math.abs(value) > limit) return true;
                }
            }
        } catch (Throwable ignored) {
            return true;
        }

        return false;
    }
}
