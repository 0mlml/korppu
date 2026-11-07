package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;

import java.util.List;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AttackManip extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Critical or Knockback mode", Mode.DoubleHop));

    public AttackManip() {
        super("AttackManip", "Executes packet manipulation for crits/kb", -1);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(StatusEntry.text("Mode", mode.getValue().name()));
    }

    @Override
    public String getStatus() {
        return mode.getValue().name();
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        if (event.getPacket() instanceof ServerboundInteractPacket) {
            Vec3 playerPos = KorppuMod.mc.player.position();
            boolean wasNofallEnabled = ModuleManager.getModule(NoFall.class).isEnabled();
            ModuleManager.getModule(NoFall.class).setEnabled(false);

            switch (mode.getValue()) {
                case DoubleHop -> handleDoubleHopCriticals(playerPos);
                case JumpReset -> handleJumpResetCriticals(playerPos);
                case MinimalLift -> handleMinimalLiftCriticals(playerPos);
                case YawKb -> handleYawKnockback(playerPos);
                case SprintKb -> handleSprintKnockback(playerPos);
                case MaceDamage -> handleMaceDamage(playerPos);
            }

            ModuleManager.getModule(NoFall.class).setEnabled(wasNofallEnabled);
        }
    }

    private void handleDoubleHopCriticals(Vec3 playerPos) {
        ServerboundMovePlayerPacket.Pos jump = new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y + 0.2,
                                                                                                   playerPos.z, true,
                                                                                                   KorppuMod.mc.player.horizontalCollision);
        ServerboundMovePlayerPacket.Pos fall = new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y,
                                                                                                   playerPos.z, false,
                                                                                                   KorppuMod.mc.player.horizontalCollision);
        ServerboundMovePlayerPacket.Pos lift = new ServerboundMovePlayerPacket.Pos(playerPos.x,
                                                                                                   playerPos.y + 0.000011,
                                                                                                   playerPos.z, false,
                                                                                                   KorppuMod.mc.player.horizontalCollision);
        ServerboundMovePlayerPacket.Pos stab = new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y,
                                                                                                   playerPos.z, false,
                                                                                                   KorppuMod.mc.player.horizontalCollision);

        sendCriticalPackets(jump, fall, lift, stab);
    }

    private void handleJumpResetCriticals(Vec3 playerPos) {
        ServerboundMovePlayerPacket.Pos reset = new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y,
                                                                                                    playerPos.z, true,
                                                                                                    KorppuMod.mc.player.horizontalCollision);
        ServerboundMovePlayerPacket.Pos jump = new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y + 0.4,
                                                                                                   playerPos.z, false,
                                                                                                   KorppuMod.mc.player.horizontalCollision);
        ServerboundMovePlayerPacket.Pos land = new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y,
                                                                                                   playerPos.z, false,
                                                                                                   KorppuMod.mc.player.horizontalCollision);

        sendCriticalPackets(reset, jump, land);
    }

    private void handleMinimalLiftCriticals(Vec3 playerPos) {
        ServerboundMovePlayerPacket.Pos subtleLift = new ServerboundMovePlayerPacket.Pos(playerPos.x,
                                                                                                         playerPos.y + 0.005,
                                                                                                         playerPos.z, false,
                                                                                                         KorppuMod.mc.player.horizontalCollision);
        ServerboundMovePlayerPacket.Pos resetPosition = new ServerboundMovePlayerPacket.Pos(playerPos.x,
                                                                                                            playerPos.y,
                                                                                                            playerPos.z, false,
                                                                                                            KorppuMod.mc.player.horizontalCollision);

        sendCriticalPackets(subtleLift, resetPosition);
    }

    private void handleYawKnockback(Vec3 playerPos) {
        KorppuMod.mc.getConnection()
                    .send(new ServerboundPlayerCommandPacket(KorppuMod.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        KorppuMod.mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(playerPos.x + 0.1 * KorppuMod.mc.player.getViewVector(1f).x,
                                                            playerPos.y + 0.05,
                                                            playerPos.z + 0.1 * KorppuMod.mc.player.getViewVector(1f).z, false,
                                                            KorppuMod.mc.player.horizontalCollision));
        KorppuMod.mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y, playerPos.z, true,
                                                            KorppuMod.mc.player.horizontalCollision));
    }

    private void handleSprintKnockback(Vec3 playerPos) {
        KorppuMod.mc.getConnection()
                    .send(new ServerboundPlayerCommandPacket(KorppuMod.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        KorppuMod.mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y + 0.1, playerPos.z, false,
                                                            KorppuMod.mc.player.horizontalCollision));
        KorppuMod.mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y, playerPos.z, true,
                                                            KorppuMod.mc.player.horizontalCollision));
    }

    private void handleMaceDamage(Vec3 playerPos) {
        if (!KorppuMod.mc.player.getMainHandItem().is(Items.MACE)) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            sendFakeY(playerPos, 0);
        }
        sendFakeY(playerPos, Math.sqrt(500));
        sendFakeY(playerPos, 0);
    }

    private void sendFakeY(Vec3 playerPos, double offset) {
        KorppuMod.mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(playerPos.x, playerPos.y + offset, playerPos.z, false,
                                                            KorppuMod.mc.player.horizontalCollision));
    }

    private void sendCriticalPackets(ServerboundMovePlayerPacket.Pos... packets) {
        for (ServerboundMovePlayerPacket.Pos packet : packets) {
            KorppuMod.mc.getConnection().send(packet);
        }
    }

    public enum Mode {
        DoubleHop, JumpReset, MinimalLift, YawKb, SprintKb, MaceDamage
    }
}
