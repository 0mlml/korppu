package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.BoatNoclip;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBoat.class)
public class AbstractBoatEntityMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void collidesWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        BoatNoclip mod = ModuleManager.getModule(BoatNoclip.class);
        if (mod == null || !mod.isEnabled()) return;

        AbstractBoat boat = (AbstractBoat) (Object) this;
        if (KorppuMod.mc.player != null && boat.getControllingPassenger() == KorppuMod.mc.player) {
            cir.setReturnValue(false);
        }
    }
}
