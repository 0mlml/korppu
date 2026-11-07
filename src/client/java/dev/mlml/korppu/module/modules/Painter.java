package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.StringSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class Painter extends Module {
    private final StringSetting blockId = config.add(new StringSetting("Block", "Block id to place (hotbar item must match)", "minecraft:white_wool"));
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Placement range", 4d, 1d, 8d, 0));
    private final DoubleSetting delayTicks = config.add(new DoubleSetting("Delay", "Ticks between placements", 2d, 0d, 20d, 0));

    private int cooldown;
    private int placed;

    public Painter() {
        super("Painter", "Auto-places a selected block on nearby surfaces", GLFW.GLFW_KEY_UNKNOWN);
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

        int slot = findBlockSlot(blockId.getValue());
        if (slot < 0) return;

        Identifier wantedBlockId;
        try {
            wantedBlockId = Identifier.parse(blockId.getValue());
        } catch (Exception e) {
            return;
        }

        int r = range.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos support = origin.offset(x, y, z);
                    BlockPos place = support.above();
                    if (origin.distSqr(support) > range.getValue() * range.getValue()) continue;

                    BlockState supportState = KorppuMod.mc.level.getBlockState(support);
                    BlockState placeState = KorppuMod.mc.level.getBlockState(place);
                    if (supportState.isAir() || !supportState.isSolidRender()) continue;
                    if (!placeState.canBeReplaced()) continue;

                    Identifier supportId = BuiltInRegistries.BLOCK.getKey(supportState.getBlock());
                    if (supportId.equals(wantedBlockId)) continue;

                    if (interact(slot, support, Direction.UP, place)) {
                        placed++;
                        cooldown = delayTicks.getValue().intValue();
                        return;
                    }
                }
            }
        }
    }

    private boolean canPlaceAt(BlockPos place) {
        if (!KorppuMod.mc.level.getBlockState(place).canBeReplaced()) return false;
        if (KorppuMod.mc.player == null) return true;

        BlockPos feet = KorppuMod.mc.player.blockPosition();
        BlockPos head = feet.above();
        return !place.equals(feet) && !place.equals(head);
    }

    private boolean interact(int slot, BlockPos support, Direction face, BlockPos hitPos) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int old = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);

        BlockHitResult bhr = new BlockHitResult(hitPos.getCenter(), face, support, false);
        var result = KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(old);
        return result != null && result.consumesAction();
    }

    private int findBlockSlot(String wantedId) {
        Identifier wanted;
        try {
            wanted = Identifier.parse(wantedId);
        } catch (Exception e) {
            return -1;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId.equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Block", blockId.getValue()),
                StatusEntry.text("Placed", String.valueOf(placed)),
                StatusEntry.text("Cooldown", String.valueOf(cooldown))
        );
    }
}
