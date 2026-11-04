package dev.glaxiblade.glaxotion;

import dev.glaxiblade.glaxotion.ui.GlaxotionScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlaxotionClient implements ClientModInitializer {
    public static final String MOD_ID = "glaxotion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static KeyMapping openScreen;

    @Override
    public void onInitializeClient() {
        GlaxotionState.IDENTITIES.load();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "general")
        );
        openScreen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.glaxotion.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_G,
                category
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GlaxotionState.TARGETS.tick(client);
            while (openScreen.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new GlaxotionScreen());
            }
        });
        LOGGER.info("Glaxotion initialized. Press G to open the local identity studio.");
    }
}
