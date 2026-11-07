package dev.mlml.korppu;

import dev.mlml.korppu.command.CommandManager;
import dev.mlml.korppu.config.ConfigWriter;
import dev.mlml.korppu.config.LiveConfig;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.event.EventManager;
import dev.mlml.korppu.item.ItemRegistry;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.player.PlayerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class KorppuMod implements ClientModInitializer {
    public static final String VERSION = FabricLoader.getInstance().getModContainer("korppu")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");

    public static final Logger LOGGER = LogManager.getLogger("Korppu");

    public static Minecraft mc;
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
        mc = Minecraft.getInstance();
        LOGGER.info("Initializing Events");
        eventManager = new EventManager();
        LOGGER.info("Initializing Modules");
        ModuleManager.init();
        LOGGER.info("Initializing Commands");
        CommandManager.init();
        LOGGER.info("Initializing Config");
        ConfigWriter.readConfigFromFile();
        List<LiveConfig.Change> uncleanChanges = LiveConfig.init();
        if (uncleanChanges != null) {
            KorppuImGui.showUncleanShutdown(uncleanChanges);
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> LiveConfig.tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigWriter.writeConfigToFile();
            LiveConfig.onCleanShutdown();
        });
        LOGGER.info("Loading Player Manager");
        PlayerManager.getInstance().load();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> PlayerManager.getInstance().save()));
        LOGGER.info("Loading Item Registry");
        ItemRegistry.load();
        LOGGER.info("Starting fast ticker");
        FAST_TICKER = new Thread(() -> {
            while (true) {
                sleep_(10);
                if (mc.player == null || mc.level == null) {
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
