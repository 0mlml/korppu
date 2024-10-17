package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.mixin.IClientPlayerInteractionManagerMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FastMine extends Module {
    private final ListSetting<Mode> order = config.add(new ListSetting<>("Order", "The order in which to break blocks", Mode.Latest));
    private final Set<Integer> sentPackets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Vec3d> posList = new ArrayList<>();

    public FastMine() {
        super("FastMine", "Instantly mine blocks", GLFW.GLFW_KEY_LEFT_BRACKET);

        KorppuMod.eventManager.register(this);
    }

    private int generatePacketIdentifier(Packet<?> packet) {
        return packet.hashCode();
    }

    private void sendPacket(Packet<?> packet) {
        if (ModuleManager.doNotSendPackets()) {
            return;
        }

        ClientConnection connection = Objects.requireNonNull(KorppuMod.mc.getNetworkHandler()).getConnection();

        if (connection == null) {
            return;
        }

        int packetId = generatePacketIdentifier(packet);
        sentPackets.add(packetId);

        connection.send(packet);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null || KorppuMod.mc.interactionManager == null) {
            return;
        }

        if (KorppuMod.mc.interactionManager.isBreakingBlock()) {
            BlockPos last = ((IClientPlayerInteractionManagerMixin) KorppuMod.mc.interactionManager).getCurrentBreakingPos();
            if (order.getValue() == Mode.Queue) {
                Vec3d p = new Vec3d(last.getX(), last.getY(), last.getZ());
                if (!posList.contains(p)) {
                    posList.add(p);
                }
            } else {
                sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, last, Direction.DOWN));
                posList.clear();
            }
        }

        Vec3d p = KorppuMod.mc.gameRenderer.getCamera().getPos();
        if (posList.isEmpty()) {
            return;
        }
        Vec3d latest = posList.get(0);
        if (latest.add(0.5, 0.5, 0.5).distanceTo(p) >= KorppuMod.mc.player.getBlockInteractionRange()) {
            posList.remove(0);
            return;
        }
        BlockPos bp = BlockPos.ofFloored(latest);
        if (Objects.requireNonNull(KorppuMod.mc.world).getBlockState(bp).isAir()) {
            posList.remove(0);
            return;
        }
        sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bp, Direction.DOWN));
        sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.DOWN));
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.world == null) {
        }


    }

    @Override
    public void onWorldRender(WorldRenderContext worldRenderContext) {
        if (!isEnabled() || posList.isEmpty()) {
            return;
        }

        VertexConsumer vc = Objects.requireNonNull(worldRenderContext.consumers()).getBuffer(RenderLayer.getLines());

        for (Vec3d p : posList) {
            WorldRenderer.drawBox(vc, (float) p.x, (float) p.y, (float) p.z, (float) p.x + 1, (float) p.y + 1, (float) p.z + 1, 1, 1, 1, 1);
        }
    }

    @Override
    public String getStatus() {
        if (order.getValue() == Mode.Latest) {
            return String.valueOf(order.getValue().name().charAt(0));
        }

        return order.getValue().name().charAt(0) + (order.getValue() == Mode.Queue ? " (" + posList.size() + ")" : "");
    }

    @Override
    public void onDisable() {
        posList.clear();
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.getPacket() instanceof PlayerActionC2SPacket packet) {
            if (sentPackets.contains(generatePacketIdentifier(packet))) {
                sentPackets.remove(generatePacketIdentifier(packet));
                return;
            }

            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK && order.getValue() == Mode.Queue) {
                event.setCancelled(true);
            }
        }
    }

    public enum Mode {
        Queue,
        Latest
    }
}
