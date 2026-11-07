package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SuperInstaMine extends Module {
    private final BooleanSetting swingHand = config.add(new BooleanSetting(
            "Swing Hand", "Visually swing hand when breaking", true));
    private final ListSetting<PatternMode> pattern = config.add(new ListSetting<>(
            "Pattern", "How additional blocks are selected", PatternMode.AdjacentCube));
    private final DoubleSetting radius = config.add(new DoubleSetting(
            "Radius", "Number of additional adjacent blocks to break (0 = single block)", 0d, 0d, 3d, 0));
    private final BooleanSetting includeCenter = config.add(new BooleanSetting(
            "Include Center", "When using sphere pattern, include center block", true)
            .showOnlyIf(() -> pattern.getValue() == PatternMode.Sphere));

    private boolean processingBreak = false;

    public SuperInstaMine() {
        super("SuperInstaMine", "Instantly mines blocks by sending start+stop break packets simultaneously", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled()) return;
        if (processingBreak) return;
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;
        if (!(event.getPacket() instanceof ServerboundPlayerActionPacket packet)) return;
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return;

        BlockPos center = packet.getPos();
        Direction dir = packet.getDirection();

        processingBreak = true;

        try {
            if (swingHand.getValue()) {
                KorppuMod.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
            }

            KorppuMod.mc.getConnection().send(
                    new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, center, dir));

            int r = (radius.getValue().intValue());
            if (r > 0) {
                List<BlockPos> extra = pattern.getValue() == PatternMode.Sphere
                        ? getSphereBlocks(center, r)
                        : getAdjacentBlocks(center, r);
                for (BlockPos pos : extra) {
                    if (pattern.getValue() == PatternMode.Sphere && !includeCenter.getValue() && pos.equals(center)) continue;
                    if (!canBreak(pos)) continue;
                    KorppuMod.mc.getConnection().send(
                            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir));
                    KorppuMod.mc.getConnection().send(
                            new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                }
            }
        } finally {
            processingBreak = false;
        }
    }

    @Override
    public void onDisable() {
        processingBreak = false;
    }

    private List<BlockPos> getAdjacentBlocks(BlockPos center, int radius) {
        List<BlockPos> list = new java.util.ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    list.add(center.offset(x, y, z));
                }
            }
        }
        return list;
    }

    private List<BlockPos> getSphereBlocks(BlockPos center, int radius) {
        List<BlockPos> list = new java.util.ArrayList<>();
        int rSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > rSq) continue;
                    list.add(center.offset(x, y, z));
                }
            }
        }
        return list;
    }

    private boolean canBreak(BlockPos pos) {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return false;
        var state = KorppuMod.mc.level.getBlockState(pos);
        if (state.isAir()) return false;
        if (KorppuMod.mc.player.getAbilities().instabuild) return true;
        ItemStack held = KorppuMod.mc.player.getMainHandItem();
        return !held.isEmpty() && held.isCorrectToolForDrops(state);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Swing", swingHand.getValue()),
                StatusEntry.text("Pattern", pattern.getValue().name()),
                StatusEntry.text("Radius", (radius.getValue().intValue()) + " blocks")
        );
    }

    @Override
    public String getStatus() {
        int r = (radius.getValue().intValue());
        return r == 0 ? "Single block" : pattern.getValue().name() + " r" + r;
    }

    public enum PatternMode {
        AdjacentCube,
        Sphere
    }
}
