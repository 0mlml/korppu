package dev.mlml.korppu.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface IPlayerInteractEntityC2SPacketMixin {
    @Accessor("entityId")
    int getEntityId();
}
