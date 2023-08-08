package dev.mlml.korppu;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KorppuMod implements ModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger("Korppu");

    @Override
    public void onInitialize()
    {
        LOGGER.info("Hello from Korppu!");
    }
}
