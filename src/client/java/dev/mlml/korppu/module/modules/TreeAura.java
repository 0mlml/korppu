package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TreeAura extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Planting range", 4d, 1d, 8d, 0));
    private final BooleanSetting bonemeal = config.add(new BooleanSetting("Bonemeal", "Use bonemeal on saplings", true));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between actions", 3d, 0d, 20d, 0));

    private int cooldown;
    private int planted;

    public TreeAura() {
        super("TreeAura", "Plants saplings automatically nearby", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos ground = origin.offset(x, y, z);
                    if (origin.distSqr(ground) > range.getValue() * range.getValue()) continue;
                    BlockPos plantPos = ground.above();

                    if (tryPlant(ground, plantPos)) return;
                    if (bonemeal.getValue() && tryBonemeal(plantPos)) return;
                }
            }
        }
    }

    private boolean tryPlant(BlockPos groundPos, BlockPos plantPos) {
        BlockState ground = KorppuMod.mc.level.getBlockState(groundPos);
        Block block = ground.getBlock();
        if (block != Blocks.DIRT && block != Blocks.GRASS_BLOCK && block != Blocks.PODZOL
                && block != Blocks.COARSE_DIRT && block != Blocks.MOSS_BLOCK) return false;
        if (!KorppuMod.mc.level.getBlockState(plantPos).isAir()) return false;

        int slot = findSaplingSlot();
        if (slot < 0) return false;

        interact(slot, groundPos, plantPos);
        planted++;
        cooldown = delayTicks.getValue().intValue();
        return true;
    }

    private boolean tryBonemeal(BlockPos pos) {
        Block block = KorppuMod.mc.level.getBlockState(pos).getBlock();
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (!id.endsWith("_sapling") && !id.equals("minecraft:mangrove_propagule") && !id.endsWith("azalea")) {
            return false;
        }

        int slot = findSlot(Items.BONE_MEAL);
        if (slot < 0) return false;

        interact(slot, pos, pos);
        cooldown = delayTicks.getValue().intValue();
        return true;
    }

    private void interact(int slot, BlockPos target, BlockPos hitPos) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        BlockHitResult bhr = new BlockHitResult(hitPos.getCenter(), Direction.UP, target, false);
        KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(old);
    }

    private int findSaplingSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.endsWith("_sapling") || id.equals("minecraft:mangrove_propagule") || id.equals("minecraft:azalea") || id.equals("minecraft:flowering_azalea")) {
                return i;
            }
        }
        return -1;
    }

    private int findSlot(Item wanted) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == wanted) return i;
        }
        return -1;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Planted", String.valueOf(planted)),
                StatusEntry.text("Cooldown", String.valueOf(cooldown)),
                StatusEntry.indicator("Bonemeal", bonemeal.getValue())
        );
    }
}
