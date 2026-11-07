package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class PortalGodMode extends Module {
    private boolean godMode = false;

    public PortalGodMode() {
        super("PortalGodMode", "Prevents teleportation through portals", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        godMode = false;
    }

    @Listener
    public void onPacketReceived(PacketEvent.Received event) {
        if (!isEnabled()) return;
        if (event.getPacket() instanceof ClientboundRespawnPacket respawn && respawn.dataToKeep() != 0) {
            godMode = true;
        }
    }

    @Listener
    public void onPacketSent(PacketEvent.Sent event) {
        if (!isEnabled()) return;
        if (godMode && event.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
            event.cancel();
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(StatusEntry.indicator("Blocking portal", godMode));
    }

    @Override
    public String getStatus() {
        return godMode ? "Blocking portal" : "Watching";
    }
}
