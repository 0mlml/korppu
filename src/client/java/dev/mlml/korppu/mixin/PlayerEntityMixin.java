package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.ShouldNoClip;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;noClip:Z", opcode = Opcodes.PUTFIELD))
    void noClip_(PlayerEntity playerEntity, boolean value) {
        ShouldNoClip snc = new ShouldNoClip(playerEntity, playerEntity.isSpectator());
        KorppuMod.eventManager.trigger(snc);
        playerEntity.noClip = snc.isShouldNoclip();
    }
}
