package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.event.events.ShouldNoClipEvent;
import dev.mlml.korppu.misc.FakePlayerEntity;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;

import java.util.List;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    private final DoubleSetting speed = config.add(new DoubleSetting("Fly Speed", "The speed at which you fly", 0.05d, 0.01d, 5d, 1));
    @Getter
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "The mode of freecam", Mode.Spectator));
    private final BooleanSetting restrictInteractions = config.add(new BooleanSetting("Restrict Interactions", "Restrict interactions with the world", true));

    Vec3 previous;
    float pitch = 0f;
    float yaw = 0f;

    private FakePlayerEntity fakePlayer;

    public Freecam() {
        super("Freecam", "Allows you to move your camera freely", GLFW.GLFW_KEY_G);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.text("Speed", String.format("%.2f", speed.getValue())),
                StatusEntry.indicator("Restrict", restrictInteractions.getValue()));
    }

    @Override
    public String getStatus() {
        return String.format("%s, %s", mode.getValue()
                                           .name()
                                           .charAt(0), restrictInteractions.getValue() ? "Restrict" : "Unbound");
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        previous = KorppuMod.mc.player.position();
        yaw = KorppuMod.mc.player.getYRot();
        pitch = KorppuMod.mc.player.getXRot();

        fakePlayer = new FakePlayerEntity();


    }

    @Override
    public void onDisable() {
        if (fakePlayer != null) {
            fakePlayer.resetPlayerPosition();
            fakePlayer.despawn();
        }

        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        if (previous == null) {
            return;
        }

        KorppuMod.mc.player.setPos(previous.x, previous.y, previous.z); KorppuMod.mc.player.setYRot(yaw); KorppuMod.mc.player.setXRot(pitch);
        previous = null;
        yaw = 0f;
        pitch = 0f;

        if (mode.getValue() == Mode.Spectator) {
            KorppuMod.mc.player.getAbilities().flying = false;
            KorppuMod.mc.player.getAbilities().setFlyingSpeed(0.05f);
        }

        KorppuMod.mc.player.setDeltaMovement(0, 0, 0);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        if (mode.getValue() == Mode.Spectator) {
            KorppuMod.mc.player.getAbilities().flying = true;
            KorppuMod.mc.player.getAbilities().setFlyingSpeed((float) (speed.getValue() + 0f));
            KorppuMod.mc.player.setSwimming(false);
        }
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent pe) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        if (pe.getPacket() instanceof ServerboundMovePlayerPacket) {
            pe.setCancelled(true);
        }
        if (pe.getPacket() instanceof ServerboundPlayerInputPacket && restrictInteractions.getValue()) {
            pe.setCancelled(true);
        }
    }

    @Listener
    public void onShouldNoclip(ShouldNoClipEvent snce) {
        if (!isEnabled() || mode.getValue() != Mode.Spectator || snce.getPlayer().onGround()) {
            return;
        }

        snce.setShouldNoclip(true);
    }

    public enum Mode {
        Spectator,
        Phantom
    }
}
