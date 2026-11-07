package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.ChatSendEvent;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Inject(at = @At("HEAD"), method = "handleChatInput", cancellable = true)
    public void sendMessage_(String chatText, boolean addToHistory, CallbackInfo ci) {
        ChatSendEvent chatSendEvent = new ChatSendEvent(chatText);
        KorppuMod.eventManager.trigger(chatSendEvent);

        if (chatSendEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        String newMessage = chatSendEvent.getMessage();
        if (addToHistory) {
            KorppuMod.mc.gui.getChat().addRecentChat(newMessage);
        }

        if (KorppuMod.mc.player == null) {
            return;
        }

        if (newMessage.startsWith("/")) {
            KorppuMod.mc.player.connection.sendCommand(newMessage.substring(1));
        } else {
            KorppuMod.mc.player.connection.sendChat(newMessage);
        }
        ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        input.setMaxLength(
                ModuleManager.getModule(Passives.class).isEnabled()
                && ModuleManager.getModule(Passives.class).getInfiniteChatLength().getValue()
                        ? Integer.MAX_VALUE : 256);
    }
}
