package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module
{
    private final ListSetting<Mode> mode = config.add(new ListSetting<Mode>("Mode", "Method", Mode.Packet));
    private final DoubleSetting fallDistance = config.add(new DoubleSetting("Fall Distance", "The distance you can fall before triggering", 2.0, 1.0, 10.0, 1));
    private boolean hasSentOnGround = false;

    public NoFall()
    {
        super("NoFall", "Prevents fall damage", ModuleType.PLAYER, GLFW.GLFW_KEY_N);

        NoFallPacketHandler packetHandler = new NoFallPacketHandler();
        KorppuMod.eventManager.register(packetHandler);
    }

    @Override
    public String getStatus()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null || KorppuMod.mc.player.fallDistance <= 0.1f)
        {
            hasSentOnGround = false;
            return mode.getValue().name();
        }

        String distanceString = String.format("%.2f", KorppuMod.mc.player.fallDistance);
        TextFormatter.Code color = KorppuMod.mc.player.fallDistance - fallDistance.getValue() < 0 ? TextFormatter.Code.GREEN : TextFormatter.Code.RED;
        if (hasSentOnGround && color == TextFormatter.Code.RED)
        {
            color = TextFormatter.Code.YELLOW;
        }

        return mode.getValue().name() + " " + TextFormatter.format("%1%s", color, distanceString);
    }

    @Override
    public void onTick()
    {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null)
        {
            return;
        }

        if (KorppuMod.mc.player.fallDistance > fallDistance.getValue())
        {
            switch (mode.getValue())
            {
                case Packet ->
                {
                    KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                    hasSentOnGround = true;
                }
                case Velocity ->
                {
                    KorppuMod.mc.player.setVelocity(0, 0.1, 0);
                    KorppuMod.mc.player.fallDistance = 0;
                }
            }
        }
    }

    public enum Mode
    {
        Packet,
        OnGround,
        Velocity
    }

    public class NoFallPacketHandler
    {
        public void onPacketSend(PacketEvent.Sent packetEvent)
        {
            if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null)
            {
                return;
            }

            if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket && mode.getValue() == Mode.OnGround)
            {
                ((IPlayerMoveC2SPacketMixin) packetEvent.getPacket()).setOnGround(true);
                hasSentOnGround = true;
            }
        }
    }
}

