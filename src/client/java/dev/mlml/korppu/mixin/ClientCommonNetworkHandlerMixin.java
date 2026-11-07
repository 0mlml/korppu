package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.GhostMode;
import dev.mlml.korppu.module.modules.SilentDisconnect;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl")
public class ClientCommonNetworkHandlerMixin {
    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true, require = 0)
    private void korppu$silentDisconnect(DisconnectionDetails details, CallbackInfo ci) {
        GhostMode ghostMode = ModuleManager.getModule(GhostMode.class);
        if (ghostMode != null) ghostMode.deactivate();

        SilentDisconnect module = ModuleManager.getModule(SilentDisconnect.class);
        if (module == null || !module.isEnabled()) {
            return;
        }

        if (KorppuMod.mc.level != null && KorppuMod.mc.player != null) {
            ChatHelper.message("Lost connection (suppressed disconnect screen).");
            ci.cancel();
        }
    }
}
