package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AntiHunger extends Module {
    private final BooleanSetting sprint = config.add(new BooleanSetting(
            "Sprint",
            "Spoofs sprinting packets",
            true
    ));
    private final BooleanSetting onGround = config.add(new BooleanSetting(
            "On Ground",
            "Spoofs the onGround flag",
            true
    ));

    private boolean lastOnGround;
    private boolean ignorePacket;

    public AntiHunger() {
        super("AntiHunger", "Reduces hunger consumption", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null) return;
        lastOnGround = KorppuMod.mc.player.onGround();
        ignorePacket = false;
    }

    @Override
    public void onDisable() {
        ignorePacket = false;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null) return;

        if (KorppuMod.mc.player.onGround() && !lastOnGround && onGround.getValue()) {
            ignorePacket = true;
        }

        lastOnGround = KorppuMod.mc.player.onGround();
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;

        if (ignorePacket && event.getPacket() instanceof ServerboundMovePlayerPacket) {
            ignorePacket = false;
            return;
        }

        if (KorppuMod.mc.player.isPassenger() || KorppuMod.mc.player.isInWater() || KorppuMod.mc.player.isUnderWater()) {
            return;
        }

        if (sprint.getValue() && event.getPacket() instanceof ServerboundPlayerCommandPacket commandPacket) {
            if (commandPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
                event.cancel();
                return;
            }
        }

        if (onGround.getValue() && event.getPacket() instanceof ServerboundMovePlayerPacket movePacket
                && KorppuMod.mc.player.onGround()
                && KorppuMod.mc.player.fallDistance <= 0.0f
                && !KorppuMod.mc.gameMode.isDestroying()) {
            ((IPlayerMoveC2SPacketMixin) movePacket).setOnGround(false);
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Sprint", sprint.getValue()),
                StatusEntry.indicator("OnGround", onGround.getValue())
        );
    }
}
