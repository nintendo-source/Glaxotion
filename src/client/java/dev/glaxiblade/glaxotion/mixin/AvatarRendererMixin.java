package dev.glaxiblade.glaxotion.mixin;

import dev.glaxiblade.glaxotion.render.IdentityRenderHooks;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies local-only skin substitutions while vanilla extracts player render state. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void glaxotion$applyLocalSkin(AbstractClientPlayer player, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        var replacement = IdentityRenderHooks.skinFor(player.getUUID());
        if (replacement != null) state.skin = replacement;
    }
}
