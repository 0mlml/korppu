package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.mixin.IClientPlayerInteractionManagerMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FastMine extends Module {
    private final ListSetting<Mode> order = config.add(new ListSetting<>("Order", "The order in which to break blocks", Mode.Latest));
    private final BooleanSetting onlyWithTool = config.add(new BooleanSetting("Only With Tool", "Only fastmine when current tool is suitable for the block", true));
    private final Set<Integer> sentPackets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Vec3> posList = new ArrayList<>();

    public FastMine() {
        super("FastMine", "Instantly mine blocks", GLFW.GLFW_KEY_LEFT_BRACKET);

        KorppuMod.eventManager.register(this);
    }

    private int generatePacketIdentifier(Packet<?> packet) {
        return packet.hashCode();
    }

    private void sendPacket(Packet<?> packet) {
        if (!ModuleManager.isSendPackets()) {
            return;
        }

        Connection connection = Objects.requireNonNull(KorppuMod.mc.getConnection()).getConnection();

        if (connection == null) {
            return;
        }

        int packetId = generatePacketIdentifier(packet);
        sentPackets.add(packetId);

        connection.send(packet);
    }

    public long calculateBreakingTime(Block block) {
        if (block.defaultDestroyTime() == -1) {
            return 0;
        }

        ItemStack is = KorppuMod.mc.player.getMainHandItem();
        float mult = is.isEmpty() ? 1 : is.getItem().getDestroySpeed(is, block.defaultBlockState());

        long time = Math.round(block.defaultDestroyTime() * 1000 / mult);

        return time;
    }

    private boolean canFastMine(BlockPos pos) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return false;
        if (!onlyWithTool.getValue()) return true;
        if (KorppuMod.mc.player.getAbilities().instabuild) return true;

        BlockState state = KorppuMod.mc.level.getBlockState(pos);
        if (state.isAir()) return false;

        ItemStack held = KorppuMod.mc.player.getMainHandItem();
        return !held.isEmpty() && held.isCorrectToolForDrops(state);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) {
            return;
        }

        if (KorppuMod.mc.gameMode.isDestroying()) {
            BlockPos last = ((IClientPlayerInteractionManagerMixin) KorppuMod.mc.gameMode).getCurrentBreakingPos();
            if (!canFastMine(last)) return;
            if (order.getValue() == Mode.Queue) {
                Vec3 p = new Vec3(last.getX(), last.getY(), last.getZ());
                if (!posList.contains(p)) {
                    posList.add(p);
                }
            } else {
                sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, last, Direction.DOWN));
                posList.clear();
            }
        }

        Vec3 p = KorppuMod.mc.gameRenderer.getMainCamera().position();
        if (posList.isEmpty()) {
            return;
        }
        Vec3 latest = posList.getFirst();
        if (latest.add(0.5, 0.5, 0.5).distanceTo(p) >= KorppuMod.mc.player.blockInteractionRange()) {
            posList.removeFirst();
            return;
        }
        BlockPos bp = BlockPos.containing(latest);
        if (!canFastMine(bp)) {
            posList.removeFirst();
            return;
        }
        if (Objects.requireNonNull(KorppuMod.mc.level).getBlockState(bp).isAir()) {
            posList.removeFirst();
            return;
        }
        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, bp, Direction.DOWN));
        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, bp, Direction.DOWN));
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
        }
    }

    @Override
    public void onWorldRender(PoseStack matrixStack, float tickProgress) {
        if (!isEnabled() || posList.isEmpty()) {
            return;
        }

        int c = 0x50b00ba5;
        for (Vec3 p : posList) {
            AABB b = new AABB(p.x, p.y, p.z, p.x + 1, p.y + 1, p.z + 1);
            RenderUtils.drawSolidBox(matrixStack, b, c , true);
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        if (order.getValue() == Mode.Queue) {
            return List.of(
                    StatusEntry.text("Order", "Queue"),
                    StatusEntry.indicator("Tool", onlyWithTool.getValue()),
                    StatusEntry.text("Queued", posList.size() + " blocks"));
        }
        return List.of(
                StatusEntry.text("Order", "Latest"),
                StatusEntry.indicator("Tool", onlyWithTool.getValue())
        );
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
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            return;
        }

        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
            if (sentPackets.contains(generatePacketIdentifier(packet))) {
                sentPackets.remove(generatePacketIdentifier(packet));
                return;
            }

            if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && order.getValue() == Mode.Queue) {
                if (!canFastMine(packet.getPos())) return;
                event.setCancelled(true);
            }
        }
    }

    public enum Mode {
        Queue,
        Latest
    }
}
