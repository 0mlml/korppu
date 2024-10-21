package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.ShouldNoClip;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Speed;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;noClip:Z", opcode = Opcodes.PUTFIELD))
    void noClip_(PlayerEntity playerEntity, boolean value) {
        ShouldNoClip snc = new ShouldNoClip(playerEntity, playerEntity.isSpectator());
        KorppuMod.eventManager.trigger(snc);
        playerEntity.noClip = snc.isShouldNoclip();
    }

    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    void writeSpeed(CallbackInfoReturnable<Float> cir) {
        Speed speed = ModuleManager.getModule(Speed.class);
        if (!speed.isEnabled() || !equals(KorppuMod.mc.player) || !speed.getMode().getValue().equals(Speed.Mode.Vanilla)) {
            return;
        }
        cir.setReturnValue((float) (cir.getReturnValue() * speed.getVanillaSpeed().getValue()));
    }
}
