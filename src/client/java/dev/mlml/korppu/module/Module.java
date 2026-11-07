package dev.mlml.korppu.module;
import com.mojang.blaze3d.platform.InputConstants;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.Config;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.imgui.windows.ModuleControlWindow;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.DeltaTracker;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final KeyMapping keybind;
    @Getter
    private final Map<Integer, Boolean> modifierKeyStates;
    @Getter
    protected Config config;
    @Getter
    private boolean enabled;
    @Getter
    @Setter
    private Category category = Category.UNSORTED;
    @Getter
    public final static KeyMapping.Category kbCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("korppu", "category"));

    public Module(String name, String description, int key) {
        this.name = name;
        this.description = description;

        keybind = new KeyMapping("key.korppumod." + name.replaceAll(" ", "")
                .toLowerCase() + "_toggle", key, kbCategory);

        config = new Config();

        modifierKeyStates = new HashMap<>();
        initializeStates();
    }

    private void initializeStates() {
        int[] keys = new int[]{GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT};

        for (int key : keys) {
            modifierKeyStates.put(key, false);
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void update(Minecraft mc) {
        initializeStates();
        if (keybind.consumeClick()) {
            modifierKeyStates.forEach((keyCode, value) -> {
                boolean isPressed = InputConstants.isKeyDown(mc.getWindow(), keyCode);
                modifierKeyStates.put(keyCode, isPressed);
            });
            if (modifierKeyStates.get(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                KorppuImGui.openModuleConfig(this);
            } else {
                if (!modifierKeyStates.containsValue(true)) {
                    toggle();
                }
            }
        }


        if (!isEnabled()) {
            return;
        }
        onTick();
    }

    public void afterUpdate(Minecraft mc) {
        if (!isEnabled()) {
            return;
        }
        afterTick();
    }

    public void onTick() {
    }

    public void afterTick() {
    }

    public void onWorldTick(ClientLevel clientWorld) {
    }

    public void onFastTick() {
    }

    public void onWorldRender(PoseStack matrixStack, float tickProgress) {
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return "";
    }

    public List<StatusEntry> getStatusEntries() {
        return List.of();
    }

    public boolean hasControlWindow() {
        return false;
    }

    public ModuleControlWindow createControlWindow() {
        return null;
    }
}
