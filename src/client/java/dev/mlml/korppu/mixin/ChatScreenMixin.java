package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.ChatSendEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "sendMessage(Ljava/lang/String;Z)V", cancellable = true)
    public void sendMessage_(String chatText, boolean addToHistory, CallbackInfo ci) {
        ChatSendEvent chatSendEvent = new ChatSendEvent(chatText);
        KorppuMod.eventManager.trigger(chatSendEvent);

        if (chatSendEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        String newMessage = chatSendEvent.getMessage();
        if (addToHistory) {
            KorppuMod.mc.inGameHud.getChatHud().addToMessageHistory(newMessage);
        }

        if (KorppuMod.mc.player == null) {
            return;
        }

        if (newMessage.startsWith("/")) {
            KorppuMod.mc.player.networkHandler.sendChatCommand(newMessage.substring(1));
        } else {
            KorppuMod.mc.player.networkHandler.sendChatMessage(newMessage);
        }

        ci.cancel();
    }
}
