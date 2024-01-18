package dev.mlml.korppu.mixin;

import dev.mlml.korppu.event.EventManager;
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
public class ClientConnectionMixin {
    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    public void exceptionCaught_(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        ex.printStackTrace();
        if (((OnlineProtections) Objects.requireNonNull(ModuleManager.getModule(OnlineProtections.class))).isAntiPacketKick()) {
            ci.cancel();
       }
    }
}
