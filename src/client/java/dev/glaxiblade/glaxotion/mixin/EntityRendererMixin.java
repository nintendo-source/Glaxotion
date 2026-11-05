package dev.glaxiblade.glaxotion.mixin;

import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Writes custom labels into the client render state after vanilla extracts them. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void glaxotion$replaceLocalNameTag(Entity entity, EntityRenderState state,
                                               float partialTick, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        IdentityProfile profile = GlaxotionState.IDENTITIES.activeProfile(player.getUUID());
        if (profile == null) return;
        String name = profile.showDisplayedName() && !profile.displayedName().isBlank()
                ? profile.displayedName() : player.getName().getString();
        String tier = profile.showTier() ? profile.visibleTierText() : "";
        String label = tier.isBlank() ? name : "[" + tier + "] " + name;
        state.nameTag = Component.literal(label)
                .withStyle(style -> style.withColor(profile.textColor()));
    }
}
