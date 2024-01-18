package dev.mlml.korppu;

import dev.mlml.korppu.gui.screens.ClientConfigScreen;
import dev.mlml.korppu.module.ModuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class KorppuMod implements ModInitializer
{
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger("Korppu");
    public static MinecraftClient mc;

    private static Thread FAST_TICKER;

    private static void sleep_(int ms)
    {
        try
        {
            Thread.sleep(ms);
        } catch (InterruptedException e)
        {
            LOGGER.warn("Interrupted while sleeping");
        }
    }

    @Override
    public void onInitialize()
    {
        LOGGER.info("Initializing Korppu");
        mc = MinecraftClient.getInstance();
        LOGGER.info("Initializing Modules");
        ModuleManager.init();
        LOGGER.info("Binding ConfigScreen");
        KeyBinding configScreenKeybind = new KeyBinding("key.korppumod.config_screen", GLFW.GLFW_KEY_MINUS, "category.korppumod");
        KeyBindingHelper.registerKeyBinding(configScreenKeybind);
        ClientTickEvents.START_CLIENT_TICK.register(client ->
        {
            if (configScreenKeybind.wasPressed())
            {
                mc.setScreen(new ClientConfigScreen());
            }
        });
        LOGGER.info("Starting fast ticker");
        FAST_TICKER = new Thread(() ->
        {
            while (true)
            {
                sleep_(10);
                if (mc.player == null || mc.world == null)
                {
                    continue;
                }
                ModuleManager.getModules().forEach(module ->
                {
                    if (module.isEnabled())
                    {
                        module.onTick();
                    }
                });
            }
        }, "Fast ticker");
        FAST_TICKER.start();
        LOGGER.info("Initialized Korppu");
    }
}
