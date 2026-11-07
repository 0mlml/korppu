package dev.mlml.korppu.item;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ItemPresetGenerator {
    ItemStack generate(ItemPresetContext context);
}
