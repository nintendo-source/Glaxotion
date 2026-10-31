package dev.glaxiblade.glaxotion.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Local profile collection associated with a player's stable UUID.
 */
public final class PlayerIdentity {
    private final UUID playerId;
    private final List<IdentityProfile> profiles = new ArrayList<>();
    private UUID activeProfileId;

    public PlayerIdentity(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
    }

    public UUID playerId() {
        return playerId;
    }

    public List<IdentityProfile> profiles() {
        return List.copyOf(profiles);
    }

    public UUID activeProfileId() {
        return activeProfileId;
    }

    public IdentityProfile activeProfile() {
        return profiles.stream()
                .filter(profile -> profile.id().equals(activeProfileId))
                .findFirst()
                .orElse(null);
    }

    public void addProfile(IdentityProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (profiles.stream().anyMatch(existing -> existing.id().equals(profile.id()))) {
            throw new IllegalArgumentException("A profile with this ID already exists.");
        }
        profiles.add(profile);
        if (activeProfileId == null) activeProfileId = profile.id();
    }

    public boolean replaceProfile(IdentityProfile replacement) {
        Objects.requireNonNull(replacement, "replacement");
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id().equals(replacement.id())) {
                profiles.set(i, replacement);
                return true;
            }
        }
        return false;
    }

    public boolean selectProfile(UUID profileId) {
        boolean exists = profiles.stream().anyMatch(profile -> profile.id().equals(profileId));
        if (!exists) return false;
        activeProfileId = profileId;
        return true;
    }

    public boolean removeProfile(UUID profileId) {
        boolean removed = profiles.removeIf(profile -> profile.id().equals(profileId));
        if (removed && Objects.equals(activeProfileId, profileId)) {
            activeProfileId = profiles.isEmpty() ? null : profiles.getFirst().id();
        }
        return removed;
    }
}
