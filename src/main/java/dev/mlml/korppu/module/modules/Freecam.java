package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.event.events.ShouldChunkRender;
import dev.mlml.korppu.event.events.ShouldNoClip;
import dev.mlml.korppu.misc.FakePlayerEntity;
import dev.mlml.korppu.module.Module;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    private final DoubleSetting speed = config.add(new DoubleSetting("Fly Speed", "The speed at which you fly", 0.05d, 0.01d, 5d, 1));
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "The mode of freecam", Mode.Spectator));
    private final BooleanSetting restrictInteractions = config.add(new BooleanSetting("Restrict Interactions", "Restrict interactions with the world", true));

    Vec3d previous;
    float pitch = 0f;
    float yaw = 0f;

    private FakePlayerEntity fakePlayer;

    public Freecam() {
        super("Freecam", "Allows you to move your camera freely", ModuleType.RENDER, GLFW.GLFW_KEY_G);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public String getStatus() {
        return String.format("%s, %s", mode.getValue()
                                           .name()
                                           .charAt(0), restrictInteractions.getValue() ? "Restrict" : "Unbound");
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null) {
            return;
        }

        previous = KorppuMod.mc.player.getPos();
        yaw = KorppuMod.mc.player.getYaw();
        pitch = KorppuMod.mc.player.getPitch();

        fakePlayer = new FakePlayerEntity();


    }

    @Override
    public void onDisable() {
        if (fakePlayer != null) {
            fakePlayer.resetPlayerPosition();
            fakePlayer.despawn();
        }

        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null) {
            return;
        }

        if (previous == null) {
            return;
        }

        KorppuMod.mc.player.updatePositionAndAngles(previous.x, previous.y, previous.z, yaw, pitch);
        previous = null;
        yaw = 0f;
        pitch = 0f;

        if (mode.getValue() == Mode.Spectator) {
            KorppuMod.mc.player.getAbilities().flying = false;
            KorppuMod.mc.player.getAbilities().setFlySpeed(0.05f);
        }

        KorppuMod.mc.player.setVelocity(0, 0, 0);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.getNetworkHandler() == null) {
            return;
        }

        if (mode.getValue() == Mode.Spectator) {
            KorppuMod.mc.player.getAbilities().flying = true;
            KorppuMod.mc.player.getAbilities().setFlySpeed((float) (speed.getValue() + 0f));
            KorppuMod.mc.player.setSwimming(false);
        }
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent pe) {
        if (!isEnabled()) {
            return;
        }

        if (pe.getPacket() instanceof PlayerMoveC2SPacket) {
            pe.setCancelled(true);
        }
        if (pe.getPacket() instanceof PlayerInputC2SPacket && restrictInteractions.getValue()) {
            pe.setCancelled(true);
        }
    }

    public void onShouldChunkRender(ShouldChunkRender scre) {
        if (!isEnabled() || mode.getValue() != Mode.Spectator) {
            return;
        }

        scre.setShouldRender(true);
    }

    public void onShouldNoclip(ShouldNoClip snce) {
        if (!isEnabled() || mode.getValue() != Mode.Spectator || snce.getPlayer().isOnGround()) {
            return;
        }

        snce.setShouldNoclip(true);
    }

    public enum Mode {
        Spectator,
        Phantom
    }
}
