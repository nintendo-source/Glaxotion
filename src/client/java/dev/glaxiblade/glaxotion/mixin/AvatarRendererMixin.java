package dev.glaxiblade.glaxotion.mixin;

import dev.glaxiblade.glaxotion.render.IdentityRenderHooks;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies local-only skin substitutions while vanilla extracts avatar render state. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void glaxotion$applyLocalSkin(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (!(avatar instanceof Player player)) return;
        var replacement = IdentityRenderHooks.skinFor(player.getUUID());
        if (replacement != null) state.skin = replacement;
    }
}
