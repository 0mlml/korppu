package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.BreakBlockEvent;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InfiniteTools extends Module {
    private final BooleanSetting autoSlot = config.add(new BooleanSetting(
            "Swap to Weakest", "Swaps to the weakest tool of the same type in your hotbar", true));
    private final DoubleSetting targetSlot = config.add(new DoubleSetting(
            "Target Slot", "Hotbar slot to swap to on block break (1-9)", 1d, 1d, 9d, 0)
            .<DoubleSetting>showOnlyIf(() -> !autoSlot.getValue()));
    private final BooleanSetting swapBack = config.add(new BooleanSetting(
            "Swap Back", "Swap back to the original slot after a short delay", true));
    private final DoubleSetting delay = config.add(new DoubleSetting(
            "Swap-Back Delay", "Ticks before swapping back to the previous slot", 1d, 1d, 20d, 0)
            .<DoubleSetting>showOnlyIf(swapBack::getValue));

    private int prevSlot = -1;
    private int dDelay = 0;

    public InfiniteTools() {
        super("InfiniteTools", "Swaps to a junk tool on block break to conserve good tool durability. Supports pickaxes, axes, shovels, hoes, and swords.", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        prevSlot = -1;
        dDelay = 0;
    }

    @Listener
    public void onBreakBlock(BreakBlockEvent event) {
        if (!isEnabled() || KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;

        BlockState state = KorppuMod.mc.level.getBlockState(event.getPos());
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int currentSlot = inv.getSelectedSlot();

        int slot = autoSlot.getValue()
                ? findToolSlot(currentSlot, state)
                : targetSlot.getValue().intValue() - 1;

        if (slot == currentSlot) return;

        if (swapBack.getValue()) prevSlot = currentSlot;

        KorppuMod.mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        inv.setSelectedSlot(slot);

        if (swapBack.getValue()) dDelay = delay.getValue().intValue();
    }

    @Override
    public void onTick() {
        if (dDelay <= 0 || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;
        dDelay--;
        if (dDelay == 0 && prevSlot != -1) {
            IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
            KorppuMod.mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
            inv.setSelectedSlot(prevSlot);
            prevSlot = -1;
        }
    }

    private int findToolSlot(int currentSlot, BlockState state) {
        var inventory = KorppuMod.mc.player.getInventory();
        ItemStack current = inventory.getItem(currentSlot);
        TagKey<Item> toolTag = getToolTag(current);
        if (toolTag == null) return currentSlot;

        float lowestSpeed = current.getDestroySpeed(state);
        int bestSlot = currentSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(toolTag)) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed < lowestSpeed) {
                lowestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private TagKey<Item> getToolTag(ItemStack stack) {
        if (stack.is(ItemTags.PICKAXES)) return ItemTags.PICKAXES;
        if (stack.is(ItemTags.AXES)) return ItemTags.AXES;
        if (stack.is(ItemTags.SHOVELS)) return ItemTags.SHOVELS;
        if (stack.is(ItemTags.HOES)) return ItemTags.HOES;
        if (stack.is(ItemTags.SWORDS)) return ItemTags.SWORDS;
        return null;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.indicator("Auto Weakest", autoSlot.getValue()));
        entries.add(StatusEntry.indicator("Swap Back", swapBack.getValue()));
        return entries;
    }

    @Override
    public String getStatus() {
        return autoSlot.getValue() ? "Auto" : "Slot " + targetSlot.getValue().intValue();
    }
}
