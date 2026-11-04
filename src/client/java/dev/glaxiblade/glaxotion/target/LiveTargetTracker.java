package dev.glaxiblade.glaxotion.target;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public final class LiveTargetTracker {
    private final TargetResolver resolver = new TargetResolver();
    private UUID currentTarget;
    public void tick(Minecraft client) {
        if (client.level == null || client.player == null) { currentTarget = null; return; }
        var loaded = new ArrayList<TrackedPlayer>();
        for (Player player : client.level.players()) loaded.add(new TrackedPlayer(player.getUUID(), client.player.distanceToSqr(player), player == client.player));
        currentTarget = resolver.resolve(loaded).orElse(null);
    }
    public Optional<UUID> currentTarget() { return Optional.ofNullable(currentTarget); }
    public void setManualTarget(UUID id) { resolver.setManualTarget(id); }
    public void clearManualTarget() { resolver.clearManualTarget(); }
    public void setRange(double blocks) { resolver.setRangeBlocks(blocks); }
    public double range() { return resolver.rangeBlocks(); }
}
