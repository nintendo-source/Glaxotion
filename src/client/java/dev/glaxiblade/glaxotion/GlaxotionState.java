package dev.glaxiblade.glaxotion;

import dev.glaxiblade.glaxotion.identity.IdentityStore;
import dev.glaxiblade.glaxotion.skin.MojangSkinService;
import dev.glaxiblade.glaxotion.target.LiveTargetTracker;

public final class GlaxotionState {
    public static final IdentityStore IDENTITIES = new IdentityStore();
    public static final LiveTargetTracker TARGETS = new LiveTargetTracker();
    public static final MojangSkinService SKINS = new MojangSkinService();
    private GlaxotionState() {}
}
