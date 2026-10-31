package dev.glaxiblade.glaxotion.target;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves manual selection first, then falls back to the nearest eligible
 * player in the current client-world snapshot.
 */
public final class TargetResolver {
    private UUID manualTargetId;
    private double rangeBlocks = 32.0;

    public Optional<UUID> manualTargetId() {
        return Optional.ofNullable(manualTargetId);
    }

    public void setManualTarget(UUID playerId) {
        manualTargetId = Objects.requireNonNull(playerId, "playerId");
    }

    public void clearManualTarget() {
        manualTargetId = null;
    }

    public double rangeBlocks() {
        return rangeBlocks;
    }

    public void setRangeBlocks(double rangeBlocks) {
        if (!Double.isFinite(rangeBlocks) || rangeBlocks < 1.0 || rangeBlocks > 256.0) {
            throw new IllegalArgumentException("Range must be between 1 and 256 blocks.");
        }
        this.rangeBlocks = rangeBlocks;
    }

    public Optional<UUID> resolve(Collection<TrackedPlayer> loadedPlayers) {
        Objects.requireNonNull(loadedPlayers, "loadedPlayers");
        double maxDistanceSquared = rangeBlocks * rangeBlocks;

        if (manualTargetId != null) {
            Optional<TrackedPlayer> manual = loadedPlayers.stream()
                    .filter(player -> !player.isLocalPlayer())
                    .filter(player -> player.id().equals(manualTargetId))
                    .filter(player -> player.distanceSquared() <= maxDistanceSquared)
                    .findFirst();
            if (manual.isPresent()) return manual.map(TrackedPlayer::id);

            // The manual target is no longer loaded or is out of range.
            manualTargetId = null;
        }

        return loadedPlayers.stream()
                .filter(player -> !player.isLocalPlayer())
                .filter(player -> player.distanceSquared() <= maxDistanceSquared)
                .min(Comparator.comparingDouble(TrackedPlayer::distanceSquared))
                .map(TrackedPlayer::id);
    }
}
