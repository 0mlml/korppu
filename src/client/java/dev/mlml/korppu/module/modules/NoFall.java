package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

import javax.swing.text.Utilities;

public class NoFall extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Method", Mode.Packet));
    private final DoubleSetting fallDistance = config.add(new DoubleSetting("Fall Distance", "The distance you can fall before triggering", 2.0, 1.0, 10.0, 1));
    private final BooleanSetting fastFall = config.add(new BooleanSetting("Fast Fall", "Intentionally take fall damage at 3 blocks (Packet only)", false));
    private boolean hasSentOnGround = false;
    private int ticksSinceSentOnGround = 0;

    public NoFall() {
        super("NoFall", "Prevents fall damage", GLFW.GLFW_KEY_N);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public String getStatus() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null || KorppuMod.mc.player.fallDistance <= 0.1f) {
            hasSentOnGround = false;
            return mode.getValue().name();
        }

        String distanceString = String.format("%.2f", KorppuMod.mc.player.fallDistance);
        TextFormatter.Code color = KorppuMod.mc.player.fallDistance - fallDistance.getValue() < 0 ? TextFormatter.Code.GREEN : TextFormatter.Code.RED;
        if (hasSentOnGround && color == TextFormatter.Code.RED) {
            color = TextFormatter.Code.YELLOW;
        }

        return mode.getValue().name() + " " + TextFormatter.format("%1%s", color, distanceString);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null) {
            return;
        }

        if (hasSentOnGround) {
            ticksSinceSentOnGround++;
        } else {
            ticksSinceSentOnGround = 0;
        }

        if (fastFall.getValue() && mode.getValue() == Mode.Packet) {
            if (KorppuMod.mc.player.fallDistance > 3.0f) {
                KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                hasSentOnGround = true;
            }
        } else {
            if (KorppuMod.mc.player.fallDistance > fallDistance.getValue()) {
                switch (mode.getValue()) {
                    case Packet -> {
                        KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                        hasSentOnGround = true;
                    }
                    case Velocity -> {
                        KorppuMod.mc.player.setVelocity(0, 0.1, 0);
                        KorppuMod.mc.player.fallDistance = 0;
                    }
                }
            }
        }
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent packetEvent) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null) {
            return;
        }

        if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket && mode.getValue() == Mode.OnGround) {
            ((IPlayerMoveC2SPacketMixin) packetEvent.getPacket()).setOnGround(true);
            hasSentOnGround = true;
        }
    }

    public enum Mode {
        Packet,
        OnGround,
        Velocity
    }
}

