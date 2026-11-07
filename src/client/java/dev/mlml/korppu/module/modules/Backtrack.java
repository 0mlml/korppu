package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.imgui.windows.BacktrackControlWindow;
import dev.mlml.korppu.imgui.windows.ModuleControlWindow;
import dev.mlml.korppu.misc.FakePlayerEntity;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.StatusEntry;
import lombok.Getter;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.IdentityHashMap;

public class Backtrack extends Module {
    @Getter
    final List<ServerboundMovePlayerPacket> packetBacklog = new ArrayList<>();
    private final DoubleSetting maxItems = config.add(new DoubleSetting("Max States", "The maximum number of movement states to buffer", 50d, 1d, 200d, 0));
    private final DoubleSetting seekBackAmount = config.add(new DoubleSetting("Seek Back Amount", "Number of states to rewind per seek-back action", 10d, 1d, 100d, 0));
    private final BooleanSetting restoreVelocity = config.add(new BooleanSetting("Restore Velocity", "Attempt to restore velocity when possible", true));
    private final BooleanSetting onlySavePositionUpdates = config.add(new BooleanSetting("Only Save Position Updates", "Ignore \"Look Only\" packets", true));
    private final Set<ServerboundMovePlayerPacket> sentPackets = Collections.newSetFromMap(new IdentityHashMap<>());
    @Getter
    private boolean catchingUp = false;
    private boolean dontRestoreOnDisable = false;

    private FakePlayerEntity fakePlayer;

    public Backtrack() {
        super("Backtrack", "Backtrack your movement", GLFW.GLFW_KEY_B);

        KorppuMod.eventManager.register(this);
    }

    public static Vec3 lerpVelocity(ServerboundMovePlayerPacket last, ServerboundMovePlayerPacket now) {
        if (last == null || now == null || KorppuMod.mc.player == null) {
            return Vec3.ZERO;
        }

        double x = last.getX(KorppuMod.mc.player.getX()) - now.getX(KorppuMod.mc.player.getX());
        double y = last.getY(KorppuMod.mc.player.getY()) - now.getY(KorppuMod.mc.player.getY());
        double z = last.getZ(KorppuMod.mc.player.getZ()) - now.getZ(KorppuMod.mc.player.getZ());
        return new Vec3(x, y, z);
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            setEnabled(false);
            return;
        }

        fakePlayer = new FakePlayerEntity();
    }

    public void seekBack() {
        if (packetBacklog.isEmpty() || seekBackAmount.getValue() > packetBacklog.size()) {
            return;
        }

        ServerboundMovePlayerPacket secondLast = null, last = null;
        for (int i = 0; i < seekBackAmount.getValue(); i++) {
            secondLast = last;
            last = packetBacklog.remove(packetBacklog.size() - 1);
        }
        moveTo(secondLast, last);
    }

    public void startCatchingUp() {
        if (KorppuMod.mc.player == null) return;
        catchingUp = true;
        if (fakePlayer != null) {
            fakePlayer.setPos(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY(), KorppuMod.mc.player.getZ());
            fakePlayer.setYRot(KorppuMod.mc.player.getYRot());
            fakePlayer.setXRot(KorppuMod.mc.player.getXRot());
        }
    }

    public void stopPlayback() {
        catchingUp = false;
    }

    public double getMaxItemsValue() {
        return maxItems.getValue();
    }

    private void moveTo(ServerboundMovePlayerPacket last, ServerboundMovePlayerPacket now) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        KorppuMod.mc.player.setPos(now.getX(KorppuMod.mc.player.getX()), now.getY(KorppuMod.mc.player.getY()), now.getZ(KorppuMod.mc.player.getZ()));
        KorppuMod.mc.player.setYRot(now.getYRot(KorppuMod.mc.player.getYRot()));
        KorppuMod.mc.player.setXRot(now.getXRot(KorppuMod.mc.player.getXRot()));

        if (!restoreVelocity.getValue()) {
            return;
        }

        Vec3 velocity = lerpVelocity(last, now);
        KorppuMod.mc.player.setDeltaMovement(velocity);
    }

    private void moveTo(ServerboundMovePlayerPacket packet) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        KorppuMod.mc.player.setPos(packet.getX(KorppuMod.mc.player.getX()), packet.getY(KorppuMod.mc.player.getY()), packet.getZ(KorppuMod.mc.player.getZ()));
        KorppuMod.mc.player.setYRot(packet.getYRot(KorppuMod.mc.player.getYRot()));
        KorppuMod.mc.player.setXRot(packet.getXRot(KorppuMod.mc.player.getXRot()));
    }

    @Override
    public void onDisable() {
        if (fakePlayer != null) {
            fakePlayer.resetPlayerPosition();
            fakePlayer.despawn();
        }

        if (dontRestoreOnDisable) {
            dontRestoreOnDisable = false;
            packetBacklog.clear();
            return;
        }

        catchingUp = false;

        if (packetBacklog.isEmpty()) {
            return;
        }

        ServerboundMovePlayerPacket first = packetBacklog.getFirst();
        packetBacklog.clear();

        if (KorppuMod.mc.player == null || first == null) {
            return;
        }

        moveTo(first);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        if (catchingUp) {
            if (packetBacklog.isEmpty()) {
                catchingUp = false;
                dontRestoreOnDisable = true;
                setEnabled(false);
                return;
            }

            ServerboundMovePlayerPacket packet = packetBacklog.removeFirst();
            if (!packetBacklog.isEmpty()) {
                moveTo(packetBacklog.getFirst(), packet);
            }
            sendPacket(packet);
        }
    }

    private void sendPacket(ServerboundMovePlayerPacket packet) {
        if (!ModuleManager.isSendPackets()) {
            return;
        }

        Connection connection = Objects.requireNonNull(KorppuMod.mc.getConnection()).getConnection();

        if (connection == null) {
            return;
        }

        sentPackets.add(packet);

        if (!catchingUp) {
            fakePlayer.setPos(packet.getX(fakePlayer.getX()), packet.getY(fakePlayer.getY()), packet.getZ(fakePlayer.getZ()));
            fakePlayer.setYRot(packet.getYRot(fakePlayer.getYRot()));
            fakePlayer.setXRot(packet.getXRot(fakePlayer.getXRot()));
        }

        connection.send(packet);
    }

    @Override
    public String getStatus() {
        String message = catchingUp ? "Replaying"
                : packetBacklog.size() >= maxItems.getValue() ? "Trailing"
                : "Standby";
        return message + " [" + packetBacklog.size() + "]";
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        String state = catchingUp ? "Replaying"
                : packetBacklog.size() >= maxItems.getValue() ? "Trailing"
                : "Standby";
        int size = packetBacklog.size();
        int max = maxItems.getValue().intValue();
        return List.of(
                StatusEntry.text("State", state),
                StatusEntry.bar("Backlog", size + " / " + max, (float) size / Math.max(1, max))
        );
    }

    @Override
    public boolean hasControlWindow() {
        return true;
    }

    @Override
    public ModuleControlWindow createControlWindow() {
        return new BacktrackControlWindow(this);
    }

    @Listener
    public void onPacket(PacketEvent.Sent event) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null || catchingUp) {
            return;
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
            if (sentPackets.remove(packet)) {
                return;
            }

            event.setCancelled(true);

            if (onlySavePositionUpdates.getValue() && !packet.hasPosition()) {
                return;
            }

            packetBacklog.add(packet);

            while (packetBacklog.size() > maxItems.getValue()) {
                ServerboundMovePlayerPacket removed = packetBacklog.removeFirst();

                sendPacket(removed);
            }

        }
    }
}
