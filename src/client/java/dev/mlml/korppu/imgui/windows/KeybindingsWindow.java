package dev.mlml.korppu.imgui.windows;
import com.mojang.blaze3d.platform.InputConstants;

import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public class KeybindingsWindow extends ImGuiWindow {
    @Override
    public WindowType getWindowType() { return WindowType.MENU; }

    private static KeybindingsWindow instance;
    private KeyMapping bindingTarget = null;
    // Set by KeyboardMixin when a key is pressed while capturing; consumed next frame.
    private InputConstants.Key capturedKey = null;

    public KeybindingsWindow() {
        instance = this;
    }

    public static boolean isCapturing() {
        return instance != null && instance.bindingTarget != null;
    }

    /** Called from KeyboardMixin while capturing, instead of polling GLFW every frame. */
    public static void onKeyCaptured(int glfwKey) {
        if (instance == null || instance.bindingTarget == null) return;
        instance.capturedKey = InputConstants.getKey(new KeyEvent(glfwKey, 0, 0));
    }
    private final ImBoolean openBool = new ImBoolean(true);

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(380, 500, ImGuiCond.Once);
        ImGui.begin("Keybindings##kbwin", openBool);

        ImGui.textDisabled("Click a key to rebind. Escape cancels.");
        ImGui.separator();

        if (ImGui.beginTable("##kbtable", 3,
                ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.RowBg)) {
            ImGui.tableSetupColumn("Action");
            ImGui.tableSetupColumn("Key");
            ImGui.tableSetupColumn("Clear");
            ImGui.tableHeadersRow();

            renderRow("ImGui Toggle", KorppuImGui.TOGGLE_KEY);
            renderRow("Console", KorppuImGui.CONSOLE_KEY);
            renderRow("Text Editor", KorppuImGui.EDITOR_KEY);
            renderRow("Context Key 1", KorppuImGui.CONTEXT_KEY_1);
            renderRow("Context Key 2", KorppuImGui.CONTEXT_KEY_2);
            renderRow("Context Key 3", KorppuImGui.CONTEXT_KEY_3);
            ImGui.tableNextRow();

            for (Module module : ModuleManager.getModules()) {
                renderRow(module.getName(), module.getKeybind());
            }

            ImGui.endTable();
        }

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
            bindingTarget = null;
        }
    }

    private void renderRow(String label, KeyMapping binding) {
        ImGui.tableNextRow();

        ImGui.tableSetColumnIndex(0);
        ImGui.text(label);

        ImGui.tableSetColumnIndex(1);
        if (bindingTarget == binding) {
            ImGui.button("[ press a key... ]##kbcap_" + label);
            InputConstants.Key pressed = captureInput();
            if (pressed != null) {
                if (pressed.equals(InputConstants.getKey(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0)))) {
                    bindingTarget = null;
                } else {
                    clearConflicts(pressed, binding);
                    binding.setKey(pressed);
                    KeyMapping.resetMapping();
                    bindingTarget = null;
                }
                capturedKey = null;
            }
        } else {
            String keyName = binding.isUnbound() ? "[ unbound ]" : binding.getTranslatedKeyMessage().getString();
            if (ImGui.button(keyName + "##kbbtn_" + label)) {
                bindingTarget = binding;
            }
        }

        ImGui.tableSetColumnIndex(2);
        if (ImGui.smallButton("X##kbclr_" + label)) {
            binding.setKey(InputConstants.UNKNOWN);
            KeyMapping.resetMapping();
            if (bindingTarget == binding) bindingTarget = null;
        }
    }

    private InputConstants.Key captureInput() {
        // Keyboard keys arrive as events via onKeyCaptured() (KeyboardMixin), so we
        // don't sweep every GLFW key each frame. Mouse buttons aren't routed through
        // that hook, so poll the 8 of them cheaply here.
        if (capturedKey != null) {
            return capturedKey;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return null;
        long handle = mc.getWindow().handle();
        for (int btn = GLFW.GLFW_MOUSE_BUTTON_1; btn <= GLFW.GLFW_MOUSE_BUTTON_8; btn++) {
            if (GLFW.glfwGetMouseButton(handle, btn) == GLFW.GLFW_PRESS) {
                return InputConstants.Type.MOUSE.getOrCreate(btn);
            }
        }

        return null;
    }

    /** If another action already holds this key, unbind it so the new binding wins. */
    private void clearConflicts(InputConstants.Key key, KeyMapping keeping) {
        if (key.equals(InputConstants.UNKNOWN)) return;
        for (KeyMapping km : allBindings()) {
            if (km == null || km == keeping) continue;
            if (key.equals(((dev.mlml.korppu.mixin.KeyMappingAccessor) (Object) km).getBoundKey())) {
                km.setKey(InputConstants.UNKNOWN);
            }
        }
    }

    private KeyMapping[] allBindings() {
        java.util.List<KeyMapping> list = new java.util.ArrayList<>();
        list.add(KorppuImGui.TOGGLE_KEY);
        list.add(KorppuImGui.CONSOLE_KEY);
        list.add(KorppuImGui.EDITOR_KEY);
        list.add(KorppuImGui.CONTEXT_KEY_1);
        list.add(KorppuImGui.CONTEXT_KEY_2);
        list.add(KorppuImGui.CONTEXT_KEY_3);
        for (Module m : ModuleManager.getModules()) list.add(m.getKeybind());
        return list.toArray(new KeyMapping[0]);
    }
}
