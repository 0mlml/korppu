package dev.mlml.korppu;

import dev.mlml.korppu.command.CommandManager;
import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.event.EventManager;
import dev.mlml.korppu.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KorppuMod implements ClientModInitializer {
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger("Korppu");

    public static MinecraftClient mc;
    public static EventManager eventManager;
    private static Thread FAST_TICKER;

    private static void sleep_(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while sleeping");
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Korppu");
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigWriter::writeConfigToFile));
        mc = MinecraftClient.getInstance();
        LOGGER.info("Initializing Events");
        eventManager = new EventManager();
        LOGGER.info("Initializing Modules");
        ModuleManager.init();
        LOGGER.info("Initializing Commands");
        CommandManager.init();
        LOGGER.info("Initializing Config");
        ConfigWriter.readConfigFromFile();
        LOGGER.info("Starting fast ticker");
        FAST_TICKER = new Thread(() -> {
            while (true) {
                sleep_(10);
                if (mc.player == null || mc.world == null) {
                    continue;
                }
                ModuleManager.getModules().forEach(module -> {
                    if (module.isEnabled()) {
                        module.onFastTick();
                    }
                });
            }
        }, "Fast ticker");
        FAST_TICKER.start();
        LOGGER.info("Initialized Korppu");
    }
}
