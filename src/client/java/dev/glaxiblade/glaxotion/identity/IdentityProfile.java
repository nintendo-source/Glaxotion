package dev.glaxiblade.glaxotion.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * A named, locally stored appearance preset for one tracked player.
 *
 * The skin source is deliberately independent from the displayed name.
 */
public record IdentityProfile(
        UUID id,
        String profileName,
        String skinSourceUsername,
        String displayedName,
        PvpTier tier,
        String customTierText,
        int textColor,
        boolean showDisplayedName,
        boolean showTier
) {
    public IdentityProfile {
        id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        profileName = clean(profileName, "Default");
        skinSourceUsername = clean(skinSourceUsername, "");
        displayedName = clean(displayedName, "");
        tier = Objects.requireNonNullElse(tier, PvpTier.CUSTOM);
        customTierText = clean(customTierText, "");
    }

    public String visibleTierText() {
        return tier == PvpTier.CUSTOM ? customTierText : tier.label();
    }

    public IdentityProfile withDisplayedName(String name) {
        return new IdentityProfile(id, profileName, skinSourceUsername, name, tier,
                customTierText, textColor, showDisplayedName, showTier);
    }

    public IdentityProfile withSkinSourceUsername(String username) {
        return new IdentityProfile(id, profileName, username, displayedName, tier,
                customTierText, textColor, showDisplayedName, showTier);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
