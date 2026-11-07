package dev.mlml.korppu.mixin;

import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.blockentity.ChestRenderer")
public class ChestBlockEntityRendererMixin {
    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/ChestRenderer;getChestMaterial(Lnet/minecraft/world/level/block/entity/BlockEntity;Z)Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;"
            ),
            index = 1,
            require = 0
    )
    private boolean korppu$forceChristmasChest(boolean original) {
        Passives passives = ModuleManager.getModule(Passives.class);
        if (passives != null && passives.isEnabled() && passives.getChristmasChests().getValue()) {
            return true;
        }
        return original;
    }
}
