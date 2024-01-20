package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.OnlineProtections;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin
{
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void handlePacket_(Packet<T> packet, PacketListener listener, CallbackInfo ci)
    {
        PacketEvent.Received pe = new PacketEvent.Received(packet);
        KorppuMod.eventManager.trigger(pe);
        if (pe.isCancelled())
        {
            ci.cancel();
        }
    }

    @Redirect(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V"))
    void replacePacket_(ClientConnection instance, Packet<?> packet, PacketCallbacks callbacks)
    {
        if (!ModuleManager.isSendPackets())
        {
            return;
        }
        PacketEvent.Sent pe = new PacketEvent.Sent(packet);
        KorppuMod.eventManager.trigger(pe);
        if (!pe.isCancelled())
        {
            instance.send(pe.getPacket(), callbacks);
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    public void exceptionCaught_(ChannelHandlerContext context, Throwable ex, CallbackInfo ci)
    {
        ex.printStackTrace();
        if (((OnlineProtections) Objects.requireNonNull(ModuleManager.getModule(OnlineProtections.class))).isAntiPacketKick())
        {
            ci.cancel();
        }
    }
}
