package dev.mlml.korppu.mixin;

import dev.mlml.korppu.imgui.ImGuiManager;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.imgui.windows.KeybindingsWindow;
import imgui.ImGui;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyEvent keyInput, CallbackInfo ci) {
        if (KeybindingsWindow.isCapturing()) {
            if (action == GLFW.GLFW_PRESS) {
                KeybindingsWindow.onKeyCaptured(keyInput.input());
            }
            ci.cancel();
            return;
        }

        if (wantCaptureKeyboard()) {
            ci.cancel();
            return;
        }

        if (!KorppuImGui.isMenuOpen()) return;

        if (keyInput.input() == GLFW.GLFW_KEY_ESCAPE) {
            if (action == GLFW.GLFW_PRESS) {
                KorppuImGui.closeMenu();
            }
            ci.cancel();
            return;
        }

        if (!isKorppuKey(keyInput)) {
            ci.cancel();
        }
    }

    private static boolean wantCaptureKeyboard() {
        return ImGuiManager.isInitialized() && ImGui.getIO().getWantCaptureKeyboard();
    }

    private static boolean isKorppuKey(KeyEvent keyInput) {
        for (KeyMapping kb : new KeyMapping[]{
                KorppuImGui.TOGGLE_KEY,
                KorppuImGui.CONSOLE_KEY,
                KorppuImGui.EDITOR_KEY,
                KorppuImGui.CONTEXT_KEY_1,
                KorppuImGui.CONTEXT_KEY_2,
                KorppuImGui.CONTEXT_KEY_3
        }) {
            if (kb != null && kb.matches(keyInput)) return true;
        }
        return false;
    }
}
