package dev.mlml.korppu;

import dev.mlml.korppu.event.EventManager;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.module.ModuleManager;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KorppuMod implements ModInitializer
{
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger("Korppu");
    @Getter
    private static final boolean sendPackets = true;
    public static MinecraftClient mc;
    public static EventManager eventManager;
    private static Thread FAST_TICKER;

    public static void addToChat(String... message)
    {
        String prefix = TextFormatter.format("[%2%3Korppu%1] ",
                TextFormatter.Code.RESET,
                TextFormatter.Code.BOLD,
                TextFormatter.Code.GRAY);
        for (String m : message)
        {
            mc.inGameHud.getChatHud().addToMessageHistory(prefix + m);
        }
    }

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
        LOGGER.info("Initializing Events");
        eventManager = new EventManager();
        LOGGER.info("Initializing Modules");
        ModuleManager.init();
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
                        module.onFastTick();
                    }
                });
            }
        }, "Fast ticker");
        FAST_TICKER.start();
        LOGGER.info("Initialized Korppu");
    }
}
