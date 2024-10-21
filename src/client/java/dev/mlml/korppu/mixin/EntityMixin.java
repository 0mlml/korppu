package dev.mlml.korppu.mixin;

import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.WallHack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    void setGlow(CallbackInfoReturnable<Boolean> cir) {
        WallHack wh = ModuleManager.getModule(WallHack.class);
        if (wh.isEnabled() && wh.getMode().getValue().equals(WallHack.Mode.Glowing)) {
            cir.setReturnValue(wh.checkShouldRender((Entity) (Object) this));
        }
    }
}
