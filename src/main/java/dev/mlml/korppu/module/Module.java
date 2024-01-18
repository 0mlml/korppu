package dev.mlml.korppu.module;

import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.glfw.GLFW;

public class Module
{
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final ModuleType type;
    @Getter
    private final KeyBinding keybind;
    @Getter
    private boolean enabled;

    public Module(String name, String description, ModuleType type, int
            key)
    {
        this.name = name;
        this.description = description;
        this.type = type;

        keybind = new KeyBinding("key.korppumod." + name.replaceAll(" ", "").toLowerCase() + "_toggle",
                key, "category.korppumod");
    }

    public void onEnable()
    {
    }

    public void onDisable()
    {
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        if (enabled)
        {
            onEnable();
        } else
        {
            onDisable();
        }
    }

    public void toggle()
    {
        setEnabled(!isEnabled());
    }

    public void update(MinecraftClient mc)
    {
        if (keybind.wasPressed())
        {
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT))
            {
                handleShiftPress();
            } else
            {
                toggle();
            }
        }


        if (!isEnabled())
        {
            return;
        }
        onTick();
    }

    public void onTick()
    {
    }

    public void onWorldTick(ClientWorld clientWorld)
    {
    }

    public void onFastTick()
    {
    }

    public void onWorldRender(WorldRenderContext worldRenderContext)
    {
    }

    public void onRender(DrawContext drawContext, float tickDelta)
    {
    }

    public String getStatus()
    {
        return "";
    }

    public void handleShiftPress()
    {
        setEnabled(!isEnabled());
    }

    public enum ModuleType
    {RENDER, PLAYER, WORLD, META, NONE}
}
