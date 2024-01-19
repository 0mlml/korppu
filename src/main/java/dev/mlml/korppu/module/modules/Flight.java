package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import net.minecraft.client.option.GameOptions;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Flight extends Module
{
    private final DoubleSetting speed = config.add(new DoubleSetting("Speed", "The speed of your flight", 1.0, 0.1, 10.0, 2));
    private final ListSetting<Mode> mode = config.add(new ListSetting<Mode>("Mode", "The mode of flight", Mode.Ability));
    private final BooleanSetting bypassVanillaAc = config.add(new BooleanSetting("Bypass Vanilla AC", "Bypass vanilla anticheat", true));
    private final BooleanSetting verboseStatus = config.add(new BooleanSetting("Verbose Status", "Show more information in status", true));
    private boolean wasFlying = false;

    public Flight()
    {
        super("Flight", "Allows you to fly", ModuleType.PLAYER, GLFW.GLFW_KEY_Z);

        FlightPacketHandler packetHandler = new FlightPacketHandler();
        KorppuMod.eventManager.register(packetHandler);
    }

    @Override
    public String getStatus()
    {
        if (verboseStatus.getValue())
        {
            return String.format("%s, %s, %s", mode.getValue().name().charAt(0), speed.getValue(), bypassVanillaAc.getValue() ? "Bypass" : "--");
        }
        return String.format("%s, %s", mode.getValue().name().charAt(0), speed.getValue());
    }

    @Override
    public void onTick()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }

        double sp = speed.getValue();

        switch (mode.getValue())
        {
            case Ability:
                KorppuMod.mc.player.getAbilities().setFlySpeed((float) sp);
                KorppuMod.mc.player.getAbilities().allowFlying = true;
                break;
            case Velocity:
                GameOptions options = KorppuMod.mc.options;
                float yaw = KorppuMod.mc.player.getYaw();
                int dx = Boolean.compare(options.rightKey.isPressed(), options.leftKey.isPressed());
                int dy = Boolean.compare(options.jumpKey.isPressed(), options.sneakKey.isPressed());
                int dz = Boolean.compare(options.backKey.isPressed(), options.forwardKey.isPressed());

                double s = Math.sin(Math.toRadians(yaw));
                double c = Math.cos(Math.toRadians(yaw));


                double nx = sp * (dz * s + dx * -c);
                double nz = sp * (dz * -c + dx * -s);
                double ny = sp * dy;

                KorppuMod.mc.player.setVelocity(new Vec3d(nx, ny, nz));
                break;
        }

        if (mode.getValue() != Mode.Ability)
        {
            KorppuMod.mc.player.getAbilities().allowFlying = wasFlying;
        }
    }

    public void onEnable()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }
        wasFlying = KorppuMod.mc.player.getAbilities().flying;
    }

    @Override
    public void onDisable()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }

        KorppuMod.mc.player.getAbilities().setFlySpeed(0.05f);
        KorppuMod.mc.player.getAbilities().allowFlying = wasFlying;
    }

    public enum Mode
    {
        Ability,
        Velocity
    }

    public class FlightPacketHandler
    {
        private boolean isHoldingModification = false;
        private long lastModificationTime = System.currentTimeMillis();
        private long endModificationCycle = System.currentTimeMillis();
        private double holdPositionY = 0;
        private double lastPositionY = 0;

        private static PlayerMoveC2SPacket.Full upgrade(PlayerMoveC2SPacket packet)
        {
            assert KorppuMod.mc.player != null;
            Vec3d pos = KorppuMod.mc.player.getPos();
            return new PlayerMoveC2SPacket.Full(
                    packet.getX(pos.x),
                    packet.getY(pos.y),
                    packet.getZ(pos.z),
                    KorppuMod.mc.player.getYaw(),
                    KorppuMod.mc.player.getPitch(),
                    packet.isOnGround()
            );
        }

        public void onPacketSend(PacketEvent.Sent packetEvent)
        {
            if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null)
            {
                return;
            }

            Packet<?> packet = packetEvent.getPacket();

            if (isShiftKeyPressed(packet))
            {
                packetEvent.setCancelled(true);
                return;
            }

            if (packet instanceof PlayerMoveC2SPacket movePacket)
            {
                if (KorppuMod.mc.player == null)
                {
                    return;
                }

                Vec3d playerPosition = KorppuMod.mc.player.getPos();
                double yPos = movePacket.getY(playerPosition.y);

                if (bypassVanillaAc.getValue())
                {
                    movePacket = processMovePacket(movePacket, playerPosition);
                    yPos = movePacket.getY(playerPosition.y);
                    packetEvent.setPacket(movePacket);

                    updatePacketPosition(movePacket, playerPosition, yPos);
                }

                lastPositionY = yPos;
            }
        }

        private boolean isShiftKeyPressed(Packet<?> packet)
        {
            return packet instanceof ClientCommandC2SPacket commandPacket
                    && commandPacket.getMode() == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY;
        }

        private PlayerMoveC2SPacket processMovePacket(PlayerMoveC2SPacket packet, Vec3d playerPosition)
        {
            if (isHoldingModification)
            {
                packet = upgrade(packet);
                if (endModificationCycle - System.currentTimeMillis() < 0)
                {
                    isHoldingModification = false;
                    endModificationCycle = System.currentTimeMillis();
                }
            } else
            {
                if (System.currentTimeMillis() - lastModificationTime > 1000)
                {
                    packet = upgrade(packet);
                    lastModificationTime = System.currentTimeMillis();
                    endModificationCycle = System.currentTimeMillis() + 50;
                    isHoldingModification = true;

                    if (!KorppuMod.mc.world.getBlockState(KorppuMod.mc.player.getBlockPos().down()).blocksMovement())
                    {
                        double delta = Math.max(0, packet.getY(playerPosition.y) - lastPositionY) + 0.05d;
                        holdPositionY = packet.getY(playerPosition.y) - delta;
                    }
                }
            }

            return packet;
        }

        private void updatePacketPosition(PlayerMoveC2SPacket packet, Vec3d playerPosition, double yPos)
        {
            IPlayerMoveC2SPacketMixin packetMixin = (IPlayerMoveC2SPacketMixin) packet;
            packetMixin.setX(packet.getX(playerPosition.x));
            packetMixin.setZ(packet.getZ(playerPosition.z));
            packetMixin.setY(isHoldingModification ? holdPositionY : yPos);
            packetMixin.setChangePosition(true);
        }
    }
}
