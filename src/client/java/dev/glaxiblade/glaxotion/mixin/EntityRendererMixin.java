package dev.glaxiblade.glaxotion.mixin;

import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the name-tag text locally, without modifying entity or server data. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void glaxotion$replaceLocalNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player)) return;
        IdentityProfile profile = GlaxotionState.IDENTITIES.activeProfile(player.getUUID());
        if (profile == null) return;
        String name = profile.showDisplayedName() && !profile.displayedName().isBlank()
                ? profile.displayedName() : player.getName().getString();
        String tier = profile.showTier() ? profile.visibleTierText() : "";
        if (tier.isBlank()) {
            cir.setReturnValue(Component.literal(name).withStyle(style -> style.withColor(profile.textColor())));
        } else {
            Component label = Component.literal("[" + tier + "] ").withStyle(style -> style.withColor(profile.textColor()));
            cir.setReturnValue(label.append(Component.literal(name).withStyle(style -> style.withColor(profile.textColor()))));
        }
    }
}
