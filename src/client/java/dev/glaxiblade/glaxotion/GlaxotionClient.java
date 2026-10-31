package dev.glaxiblade.glaxotion;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlaxotionClient implements ClientModInitializer {
    public static final String MOD_ID = "glaxotion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Glaxotion client initialized.");
    }
}
