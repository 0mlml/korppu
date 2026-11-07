package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.event.events.ShouldNoClipEvent;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import lombok.Getter;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Pose;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Flight extends Module {
    private final DoubleSetting speed = config.add(new DoubleSetting("Speed", "The speed of your flight", 1.0, 0.1, 10.0, 2));
    @Getter
    private final ListSetting<Mode> mode = config.add(new ListSetting<Mode>("Mode", "The mode of flight", Mode.Ability));
    private final BooleanSetting bypassVanillaAc = config.add(new BooleanSetting("Bypass float check", "Bypass vanilla anticheat", true));
    private final BooleanSetting verboseStatus = config.add(new BooleanSetting("Verbose Status", "Show more information in status", true));
    @Getter
    private final BooleanSetting phase = config.add(new BooleanSetting("Phase", "Tries to phase through blocks", false));

    private boolean wasFlying = false;
    private boolean ph_isHoldingModification = false;
    private long ph_lastModificationTime = System.currentTimeMillis();
    private long ph_endModificationCycle = System.currentTimeMillis();
    private double ph_holdPositionY = 0;
    private double ph_lastPositionY = 0;

    public Flight() {
        super("Flight", "Allows you to fly", GLFW.GLFW_KEY_Z);

        KorppuMod.eventManager.register(this);
    }

    private static ServerboundMovePlayerPacket.PosRot upgrade(ServerboundMovePlayerPacket packet) {
        assert KorppuMod.mc.player != null;
        Vec3 pos = KorppuMod.mc.player.position();
        return new ServerboundMovePlayerPacket.PosRot(packet.getX(pos.x), packet.getY(pos.y), packet.getZ(pos.z), KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(), packet.isOnGround(), packet.horizontalCollision());
    }

    @Override
    public String getStatus() {
        if (verboseStatus.getValue()) {
            return String.format(
                    "%s, %s, %s%s",
                    mode.getValue()
                            .name()
                            .charAt(0),
                    speed.getValue(),
                    bypassVanillaAc.getValue() ? "Bypass " : "",
                    phase.getValue() ? KorppuMod.mc.player != null && KorppuMod.mc.player.getAbilities().flying ? "Phasing" : "Phase" : "");
        }
        return String.format("%s, %s", mode.getValue().name().charAt(0), speed.getValue());
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.text("Mode", mode.getValue().name()));
        entries.add(StatusEntry.text("Speed", String.format("%.1f", speed.getValue())));
        entries.add(StatusEntry.indicator("Bypass AC", bypassVanillaAc.getValue()));
        if (phase.getValue()) {
            boolean phasing = KorppuMod.mc.player != null && KorppuMod.mc.player.getAbilities().flying;
            entries.add(StatusEntry.indicator("Phasing", phasing));
        }
        return entries;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        double sp = speed.getValue();

        switch (mode.getValue()) {
            case Ability:
                KorppuMod.mc.player.getAbilities().setFlyingSpeed((float) sp / 10f);
                KorppuMod.mc.player.getAbilities().flying = true;
                break;
            case Velocity:
                Options options = KorppuMod.mc.options;
                float yaw = KorppuMod.mc.player.getYRot();
                int dx = Boolean.compare(options.keyRight.isDown(), options.keyLeft.isDown());
                int dy = Boolean.compare(options.keyJump.isDown(), options.keyShift.isDown());
                int dz = Boolean.compare(options.keyDown.isDown(), options.keyUp.isDown());

                double s = Math.sin(Math.toRadians(yaw));
                double c = Math.cos(Math.toRadians(yaw));

                double nx = sp * (dz * s + dx * -c);
                double nz = sp * (dz * -c + dx * -s);
                double ny = sp * dy;

                KorppuMod.mc.player.setDeltaMovement(new Vec3(nx, ny, nz));
                break;
        }

        if (mode.getValue() != Mode.Ability) {
            KorppuMod.mc.player.getAbilities().flying = wasFlying;
        }
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        wasFlying = KorppuMod.mc.player.getAbilities().flying;
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        KorppuMod.mc.player.getAbilities().setFlyingSpeed(0.05f);
        KorppuMod.mc.player.getAbilities().flying = wasFlying;
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent packetEvent) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        Packet<?> packet = packetEvent.getPacket();

        if (isShiftKeyPressedInPacket(packet)) {
            packetEvent.setCancelled(true);
            return;
        }

        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            if (KorppuMod.mc.player == null) {
                return;
            }

            Vec3 playerPosition = KorppuMod.mc.player.position();
            double yPos = movePacket.getY(playerPosition.y);

            if (bypassVanillaAc.getValue()) {
                movePacket = processMovePacket(movePacket, playerPosition);
                yPos = movePacket.getY(playerPosition.y);
                packetEvent.setPacket(movePacket);

                updatePacketPosition(movePacket, playerPosition, yPos);
            }

            ph_lastPositionY = yPos;
        }
    }

    @Listener
    public void onPacketSend2(PacketEvent.Sent pe) {
        if (!phase.getValue() || KorppuMod.mc.player == null || !KorppuMod.mc.player.getAbilities().flying) {
            return;
        }
        AABB p = KorppuMod.mc.player.getDimensions(KorppuMod.mc.player.getPose()).makeBoundingBox(KorppuMod.mc.player.position()).move(0, 0.27, 0).inflate(0.25);
        if ((p.maxY - p.minY) < 2) {
            p = p.inflate(0, 1, 0);
        }
        if (pe.getPacket() instanceof ServerboundMovePlayerPacket && !Objects.requireNonNull(KorppuMod.mc.level).noCollision(KorppuMod.mc.player, p)) {
            pe.setCancelled(true);
        }
    }

    @Listener
    public void onShouldNoClip(ShouldNoClipEvent event) {
        if (event.getPlayer().getAbilities().flying) {
            event.setShouldNoclip(true);
        }
    }

    private boolean isShiftKeyPressedInPacket(Packet<?> packet) {
        return packet instanceof ServerboundPlayerCommandPacket commandPacket && commandPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING;
    }

    private ServerboundMovePlayerPacket processMovePacket(ServerboundMovePlayerPacket packet, Vec3 playerPosition) {
        if (ph_isHoldingModification) {
            packet = upgrade(packet);
            if (ph_endModificationCycle - System.currentTimeMillis() < 0) {
                ph_isHoldingModification = false;
                ph_endModificationCycle = System.currentTimeMillis();
            }
        } else {
            if (System.currentTimeMillis() - ph_lastModificationTime > 1000) {
                packet = upgrade(packet);
                ph_lastModificationTime = System.currentTimeMillis();
                ph_endModificationCycle = System.currentTimeMillis() + 50;
                ph_isHoldingModification = true;

                if (!KorppuMod.mc.level.getBlockState(KorppuMod.mc.player.blockPosition().below()).blocksMotion()) {
                    double delta = Math.max(0, packet.getY(playerPosition.y) - ph_lastPositionY) + 0.05d;
                    ph_holdPositionY = packet.getY(playerPosition.y) - delta;
                }
            }
        }

        return packet;
    }

    private void updatePacketPosition(ServerboundMovePlayerPacket packet, Vec3 playerPosition, double yPos) {
        IPlayerMoveC2SPacketMixin packetMixin = (IPlayerMoveC2SPacketMixin) packet;
        packetMixin.setX(packet.getX(playerPosition.x));
        packetMixin.setZ(packet.getZ(playerPosition.z));
        packetMixin.setY(ph_isHoldingModification ? ph_holdPositionY : yPos);
        packetMixin.setChangePosition(true);
    }

    @Override
    public void onWorldRender(PoseStack ms, float tickProgress) {
        if (phase.getValue() && Objects.requireNonNull(KorppuMod.mc.player).getAbilities().flying) {
            KorppuMod.mc.player.setPose(Pose.STANDING);
            KorppuMod.mc.player.setOnGround(false);
            KorppuMod.mc.player.fallDistance = 0;
        }
    }

    public enum Mode {
        Ability,
        Velocity,
    }
}
