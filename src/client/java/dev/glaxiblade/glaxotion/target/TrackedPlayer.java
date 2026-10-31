package dev.glaxiblade.glaxotion.target;

import java.util.UUID;

/**
 * Minimal snapshot of a player currently loaded by the local client.
 */
public record TrackedPlayer(UUID id, double distanceSquared, boolean isLocalPlayer) {
    public TrackedPlayer {
        if (id == null) throw new IllegalArgumentException("Player UUID cannot be null.");
        if (distanceSquared < 0) throw new IllegalArgumentException("Distance cannot be negative.");
    }
}
