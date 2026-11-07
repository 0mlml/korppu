package dev.mlml.korppu.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.ModuleManager;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ClientConnectionMixin {
    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void handlePacket_(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        PacketEvent.Received pe = new PacketEvent.Received(packet);
        KorppuMod.eventManager.trigger(pe);
        if (pe.isCancelled()) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V")
    private void onSend(Packet<?> packet, @Nullable ChannelFutureListener callback, Operation<Void> original) {
        if (!ModuleManager.isSendPackets()) {
            original.call(packet, callback);
            return;
        }
        PacketEvent.Sent event = new PacketEvent.Sent(packet);
        KorppuMod.eventManager.trigger(event);
        if (event.isCancelled()) {
            return;
        }
        original.call(event.getPacket(), callback);
    }
}
