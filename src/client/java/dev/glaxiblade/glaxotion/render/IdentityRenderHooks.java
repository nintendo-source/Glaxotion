package dev.glaxiblade.glaxotion.render;

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

/** Bridges saved local profiles to the player render-state skin field. */
public final class IdentityRenderHooks {
    private static final Map<UUID, PlayerSkin> SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> REQUESTED = new ConcurrentHashMap<>();
    private IdentityRenderHooks() {}

    public static PlayerSkin skinFor(UUID playerId) {
        IdentityProfile profile = GlaxotionState.IDENTITIES.activeProfile(playerId);
        if (profile == null || profile.skinSourceUsername().isBlank()) return null;
        String username = profile.skinSourceUsername();
        if (!username.equals(REQUESTED.put(playerId, username))) {
            SKINS.remove(playerId);
            GlaxotionState.SKINS.resolve(username).whenComplete((result, error) -> {
                if (error != null || result == null || result.isEmpty()) return;
                MojangSkinService.SkinData skin = result.get();
                if (!username.equals(REQUESTED.get(playerId))) return;
                Identifier textureId = Identifier.fromNamespaceAndPath("glaxotion", "skins/" + playerId.toString().replace('-', '_') + "_" + skin.uuid().toString().replace('-', '_'));
                ClientAsset.Texture texture = new ClientAsset.DownloadedTexture(textureId, skin.textureUrl());
                PlayerModelType model = skin.slimModel() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                SKINS.put(playerId, PlayerSkin.insecure(texture, null, null, model));
            });
        }
        return SKINS.get(playerId);
    }
}
