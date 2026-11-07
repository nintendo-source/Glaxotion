package dev.glaxiblade.glaxotion.mixin;

import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import dev.glaxiblade.glaxotion.render.IdentityRenderHooks;
import net.minecraft.world.entity.Avatar;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Glaxotion's local appearance overrides at the end of avatar state
 * extraction, after vanilla has populated the skin and name-tag render data.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void glaxotion$applyLocalIdentity(Avatar avatar, AvatarRenderState state,
                                               float partialTick, CallbackInfo ci) {
        var replacement = IdentityRenderHooks.skinFor(avatar.getUUID());
        if (replacement != null) state.skin = replacement;

        IdentityProfile profile = GlaxotionState.IDENTITIES.activeProfile(avatar.getUUID());
        if (profile == null) return;

        String name = profile.showDisplayedName() && !profile.displayedName().isBlank()
                ? profile.displayedName() : avatar.getName().getString();
        String tier = profile.showTier() ? profile.visibleTierText() : "";
        String label = tier.isBlank() ? name : "[" + tier + "] " + name;
        state.nameTag = Component.literal(label)
                .withStyle(style -> style.withColor(profile.textColor()));
    }
}
