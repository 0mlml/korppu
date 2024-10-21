package dev.mlml.korppu.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntityMixin {
    @Accessor("jumpingCooldown")
    void setJumpingCooldown(int c);
}
