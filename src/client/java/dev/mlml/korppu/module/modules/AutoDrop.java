package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AutoDrop extends Module {
    private final MultiSelectSetting itemList = config.add(new MultiSelectSetting(
            "Items", "Items to automatically drop",
            () -> BuiltInRegistries.ITEM.stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .filter(id -> !id.equals("minecraft:air"))
                    .sorted()
                    .collect(Collectors.toList()),
            true, false));

    private final BooleanSetting dropHotbar = config.add(new BooleanSetting("Drop Hotbar", "Also drop items in hotbar slots", false));

    public AutoDrop() {
        super("AutoDrop", "Automatically drops configured items from your inventory", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;
        if (itemList.getValue().isEmpty()) return;

        var inventory = KorppuMod.mc.player.getInventory();
        var handler = KorppuMod.mc.player.containerMenu;

        int startInvSlot = dropHotbar.getValue() ? 0 : 9;

        for (int invSlot = startInvSlot; invSlot < 36; invSlot++) {
            ItemStack stack = inventory.getItem(invSlot);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (itemList.isSelected(id)) {
                int screenSlot = invSlot < 9 ? invSlot + 36 : invSlot;
                KorppuMod.mc.gameMode.handleContainerInput(
                        handler.containerId, screenSlot, 1, ContainerInput.THROW, KorppuMod.mc.player);
            }
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.text("Watching", itemList.getValue().size() + " item types"));
        entries.add(StatusEntry.indicator("Hotbar", dropHotbar.getValue()));
        return entries;
    }

    @Override
    public String getStatus() {
        return itemList.getValue().size() + " items";
    }
}
