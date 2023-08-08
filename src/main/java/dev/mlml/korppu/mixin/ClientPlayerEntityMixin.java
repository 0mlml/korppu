package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin
{
    @Unique
    private int tickCount = 0;
    @Inject(at = @At("HEAD"), method = "tick")
    public void preTick(CallbackInfo ci)
    {
        // only log every 20 ticks
        if (tickCount++ % 20 == 0)
        {
            KorppuMod.LOGGER.info("20 ticks have passed!");
            tickCount = 1;
        }
    }
}
