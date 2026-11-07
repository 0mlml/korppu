package dev.mlml.korppu.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntityMixin {

    @Accessor("noJumpDelay")
    void setJumpingCooldown(int c);

    @Accessor("yBodyRot")
    void setBodyYaw(float yaw);

    @Accessor("yBodyRotO")
    void setLastBodyYaw(float yaw);

    @Accessor("yHeadRot")
    void setHeadYaw(float yaw);

    @Accessor("yHeadRotO")
    void setLastHeadYaw(float yaw);
}
