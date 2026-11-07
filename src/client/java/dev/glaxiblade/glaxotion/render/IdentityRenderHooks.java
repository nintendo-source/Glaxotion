package dev.glaxiblade.glaxotion.render;

import dev.glaxiblade.glaxotion.GlaxotionClient;
import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import dev.glaxiblade.glaxotion.skin.MojangSkinService;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves local identity profiles into render-state skins.
 *
 * The render hook never performs network work on the render thread. A request
 * is started once per active profile and its result is cached until refresh().
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
                Identifier textureId = Identifier.fromNamespaceAndPath(
                        "glaxotion",
                        "skins/" + playerId.toString().replace("-", "") + "_" + skin.uuid().toString().replace("-", "")
                );
                ClientAsset.Texture texture = new ClientAsset.DownloadedTexture(textureId, skin.textureUrl());
                PlayerModelType model = skin.slimModel() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                SKINS.put(playerId, PlayerSkin.insecure(texture, null, null, model));
                STATUS.put(playerId, "Skin ready: " + skin.canonicalName());
                GlaxotionClient.LOGGER.info("Glaxotion resolved skin {} for local target {}", skin.canonicalName(), playerId);
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
