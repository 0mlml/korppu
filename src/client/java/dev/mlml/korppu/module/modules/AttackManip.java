package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class AttackManip extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Critical or Knockback mode", Mode.DoubleHop));

    public AttackManip() {
        super("AttackManip", "Executes packet manipulation for crits/kb", -1);

        KorppuMod.eventManager.register(this);
    }

    @Override
    public String getStatus() {
        return mode.getValue().name();
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null) {
            return;
        }

        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            Vec3d playerPos = KorppuMod.mc.player.getPos();
            boolean wasNofallEnabled = ModuleManager.getModule(NoFall.class).isEnabled();
            ModuleManager.getModule(NoFall.class).setEnabled(false);

            switch (mode.getValue()) {
                case DoubleHop -> handleDoubleHopCriticals(playerPos);
                case JumpReset -> handleJumpResetCriticals(playerPos);
                case MinimalLift -> handleMinimalLiftCriticals(playerPos);
                case YawKb -> handleYawKnockback(playerPos);
                case SprintKb -> handleSprintKnockback(playerPos);
            }

            ModuleManager.getModule(NoFall.class).setEnabled(wasNofallEnabled);
        }
    }

    private void handleDoubleHopCriticals(Vec3d playerPos) {
        PlayerMoveC2SPacket.PositionAndOnGround jump = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y + 0.2, playerPos.z, true);
        PlayerMoveC2SPacket.PositionAndOnGround fall = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, false);
        PlayerMoveC2SPacket.PositionAndOnGround lift = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y + 0.000011, playerPos.z, false);
        PlayerMoveC2SPacket.PositionAndOnGround stab = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, false);

        sendCriticalPackets(jump, fall, lift, stab);
    }

    private void handleJumpResetCriticals(Vec3d playerPos) {
        PlayerMoveC2SPacket.PositionAndOnGround reset = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, true);
        PlayerMoveC2SPacket.PositionAndOnGround jump = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y + 0.4, playerPos.z, false);
        PlayerMoveC2SPacket.PositionAndOnGround land = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, false);

        sendCriticalPackets(reset, jump, land);
    }

    private void handleMinimalLiftCriticals(Vec3d playerPos) {
        PlayerMoveC2SPacket.PositionAndOnGround subtleLift = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y + 0.005, playerPos.z, false);
        PlayerMoveC2SPacket.PositionAndOnGround resetPosition = new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, false);

        sendCriticalPackets(subtleLift, resetPosition);
    }

    private void handleYawKnockback(Vec3d playerPos) {
        KorppuMod.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(KorppuMod.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x + 0.1 * KorppuMod.mc.player.getRotationVec(1f).x, playerPos.y + 0.05, playerPos.z + 0.1 * KorppuMod.mc.player.getRotationVec(1f).z, false));
        KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, true));
    }

    private void handleSprintKnockback(Vec3d playerPos) {
        KorppuMod.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(KorppuMod.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y + 0.1, playerPos.z, false));
        KorppuMod.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.x, playerPos.y, playerPos.z, true));
    }

    private void sendCriticalPackets(PlayerMoveC2SPacket.PositionAndOnGround... packets) {
        for (PlayerMoveC2SPacket.PositionAndOnGround packet : packets) {
            KorppuMod.mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public enum Mode {
        DoubleHop, JumpReset, MinimalLift, YawKb, SprintKb
    }
}
