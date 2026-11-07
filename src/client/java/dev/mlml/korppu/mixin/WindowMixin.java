package dev.mlml.korppu.mixin;

import dev.mlml.korppu.imgui.ImGuiManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(at = @At("HEAD"), method = "close()V")
    private void onClose(CallbackInfo ci) {
        ImGuiManager.dispose();
    }
}
