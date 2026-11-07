package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Squake;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class SquakeMixin {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void squake$travel(Vec3 movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof LocalPlayer player)) return;
        if (player != KorppuMod.mc.player) return;

        Squake squake = ModuleManager.getModule(Squake.class);
        if (squake == null || !squake.isEnabled()) return;

        if (squake.doTravel(player, movementInput)) {
            self.move(MoverType.SELF, self.getDeltaMovement());
            squake.applyGravity(player);
            ci.cancel();
        }
    }

    @Inject(method = "jump", at = @At("TAIL"), require = 0)
    private void squake$afterJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof LocalPlayer player)) return;
        if (player != KorppuMod.mc.player) return;

        Squake squake = ModuleManager.getModule(Squake.class);
        if (squake == null || !squake.isEnabled()) return;

        squake.afterJump(player);
    }
}
