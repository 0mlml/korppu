package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.mixin.IPlayerInteractEntityC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class VehicleOneHit extends Module {
    private final DoubleSetting extraHits = config.add(new DoubleSetting("Extra Hits", "Additional attack packets sent to vehicles", 3d, 1d, 20d, 0));

    private boolean replaying;

    public VehicleOneHit() {
        super("VehicleOneHit", "Repeats attack packets against boats and minecarts", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Listener
    public void onPacket(PacketEvent.Sent event) {
        if (!isEnabled() || replaying || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;
        if (!(event.getPacket() instanceof ServerboundInteractPacket packet)) return;

        int entityId = packet.entityId();
        Entity entity = KorppuMod.mc.level.getEntity(entityId);
        if (entity == null) return;

        String path = entity.getType().toString();
        if (!path.contains("boat") && !path.contains("minecart")) return;

        replaying = true;
        try {
            for (int i = 0; i < extraHits.getValue().intValue(); i++) {
                KorppuMod.mc.getConnection().send(packet);
            }
        } finally {
            replaying = false;
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(StatusEntry.text("Extra Hits", String.valueOf(extraHits.getValue().intValue())));
    }
}
