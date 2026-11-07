package dev.mlml.korppu.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface IPlayerInventoryMixin {
    @Accessor("selected")
    int getSelectedSlot();

    @Mutable
    @Accessor("selected")
    void setSelectedSlot(int slot);
}
