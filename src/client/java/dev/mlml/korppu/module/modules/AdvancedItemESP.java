package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.module.EspManager;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AdvancedItemESP extends Module {
    private final MultiSelectSetting itemList = config.add(new MultiSelectSetting(
            "Items", "Items to highlight when on the ground",
            () -> BuiltInRegistries.ITEM.stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .filter(id -> !id.equals("minecraft:air"))
                    .sorted()
                    .collect(Collectors.toList()),
            true, false));
    private final DoubleSetting range = config.add(new DoubleSetting(
            "Range", "Maximum range to search for items", 64d, 8d, 512d, 0));

    public AdvancedItemESP() {
        super("AdvancedItemESP", "Highlights specific items lying on the ground", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickDelta) {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return;
        if (itemList.getValue().isEmpty()) return;

        StreamSupport.stream(KorppuMod.mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof ItemEntity)
                .filter(e -> e.distanceTo(KorppuMod.mc.player) <= range.getValue())
                .map(e -> (ItemEntity) e)
                .filter(e -> {
                    String id = BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString();
                    return itemList.isSelected(id);
                })
                .forEach(entity -> EspManager.register(entity, 0xA000BFFF, 0xFF00BFFF, EspManager.PRIORITY_SPECIALIZED));
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return List.of();
        long count = StreamSupport.stream(KorppuMod.mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof ItemEntity)
                .filter(e -> e.distanceTo(KorppuMod.mc.player) <= range.getValue())
                .map(e -> (ItemEntity) e)
                .filter(e -> itemList.isSelected(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString()))
                .count();
        return List.of(StatusEntry.text("Visible items", count + ""), StatusEntry.text("Watching", itemList.getValue().size() + " types"));
    }

    @Override
    public String getStatus() {
        return itemList.getValue().size() + " items configured";
    }
}
