package dev.glaxiblade.glaxotion.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.glaxiblade.glaxotion.GlaxotionClient;
import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import dev.glaxiblade.glaxotion.skin.MojangSkinService;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves local identity profiles into fully registered vanilla skin assets.
 *
 * SkinManager is responsible for downloading, caching and registering the
 * texture with Minecraft's TextureManager. A DownloadedTexture descriptor by
 * itself is not a registered/renderable texture.
 */
public final class IdentityRenderHooks {
    private static final Map<UUID, PlayerSkin> SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> REQUESTED = new ConcurrentHashMap<>();
    private static final Map<UUID, String> STATUS = new ConcurrentHashMap<>();

    private IdentityRenderHooks() {}

    public static PlayerSkin skinFor(UUID playerId) {
        IdentityProfile profile = GlaxotionState.IDENTITIES.activeProfile(playerId);
        if (profile == null || profile.skinSourceUsername().isBlank()) {
            SKINS.remove(playerId);
            REQUESTED.remove(playerId);
            STATUS.remove(playerId);
            return null;
        }

        String username = profile.skinSourceUsername().trim();
        String requestKey = profile.id() + ":" + username.toLowerCase();
        if (!requestKey.equals(REQUESTED.get(playerId))) {
            REQUESTED.put(playerId, requestKey);
            SKINS.remove(playerId);
            STATUS.put(playerId, "Looking up " + username + "…");

            GlaxotionState.SKINS.resolve(username).whenComplete((result, error) -> {
                if (!requestKey.equals(REQUESTED.get(playerId))) return;

                if (error != null) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
                    STATUS.put(playerId, "Skin lookup failed: " + message);
                    GlaxotionClient.LOGGER.warn("Glaxotion could not resolve skin for {}", username, cause);
                    return;
                }

                if (result == null || result.isEmpty()) {
                    STATUS.put(playerId, "No Java skin profile found for " + username);
                    GlaxotionClient.LOGGER.warn("No Mojang skin profile found for {}", username);
                    return;
                }

                MojangSkinService.SkinData skin = result.get();
                STATUS.put(playerId, "Downloading " + skin.canonicalName() + " skin…");
                GameProfile sourceProfile = new GameProfile(skin.uuid(), skin.canonicalName());
                // Supply the exact Mojang textures property (including its
                // signature when present) so SkinManager can resolve Dream's
                // actual texture rather than falling back to the default skin.
                sourceProfile.getProperties().put("textures",
                        skin.texturePropertySignature() == null
                                ? new Property("textures", skin.texturePropertyValue())
                                : new Property("textures", skin.texturePropertyValue(), skin.texturePropertySignature()));
                Minecraft.getInstance().getSkinManager().get(sourceProfile).whenComplete((loaded, skinError) -> {
                    if (!requestKey.equals(REQUESTED.get(playerId))) return;
                    if (skinError != null) {
                        Throwable cause = skinError.getCause() == null ? skinError : skinError.getCause();
                        STATUS.put(playerId, "Skin download failed: " + cause.getClass().getSimpleName());
                        GlaxotionClient.LOGGER.warn("Glaxotion could not download/register skin for {}", username, cause);
                        return;
                    }
                    if (loaded == null || loaded.isEmpty()) {
                        STATUS.put(playerId, "Mojang returned no skin for " + username);
                        return;
                    }
                    SKINS.put(playerId, loaded.get());
                    STATUS.put(playerId, "Skin loaded: " + skin.canonicalName());
                    GlaxotionClient.LOGGER.info("Glaxotion loaded vanilla skin asset {} for local target {}", skin.canonicalName(), playerId);
                });
            });
        }

        return SKINS.get(playerId);
    }

    /** Clears a target's cached result so Save can retry a failed lookup. */
    public static void refresh(UUID playerId) {
        SKINS.remove(playerId);
        REQUESTED.remove(playerId);
        STATUS.remove(playerId);
    }

    public static String skinStatus(UUID playerId) {
        if (playerId == null) return "Select a player to edit.";
        return STATUS.getOrDefault(playerId, "Skin override not requested yet.");
    }
}
