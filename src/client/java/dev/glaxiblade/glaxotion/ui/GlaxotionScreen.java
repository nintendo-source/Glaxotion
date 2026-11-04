package dev.glaxiblade.glaxotion.ui;

import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import dev.glaxiblade.glaxotion.identity.PvpTier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

/** Local profile editor for the currently resolved or manually selected player. */
public final class GlaxotionScreen extends Screen {
    private EditBox profileName;
    private EditBox skinUsername;
    private EditBox displayedName;
    private EditBox customTier;
    private UUID targetId;
    private IdentityProfile loadedProfile;
    private PvpTier tier = PvpTier.HT3;
    private String status = "Auto Target selects the nearest loaded player.";

    public GlaxotionScreen() { super(Component.literal("Glaxotion")); }

    @Override
    protected void init() {
        int w = Math.min(400, width - 24);
        int x = (width - w) / 2;
        int y = Math.max(24, (height - 270) / 2);
        targetId = GlaxotionState.TARGETS.currentTarget().orElse(null);
        loadedProfile = targetId == null ? null : GlaxotionState.IDENTITIES.activeProfile(targetId);

        profileName = new EditBox(font, x + 18, y + 55, w - 36, 20, Component.literal("Profile name"));
        skinUsername = new EditBox(font, x + 18, y + 99, w - 36, 20, Component.literal("Skin source username"));
        displayedName = new EditBox(font, x + 18, y + 143, w - 36, 20, Component.literal("Displayed IGN"));
        customTier = new EditBox(font, x + 18, y + 187, w - 36, 20, Component.literal("Custom tier"));
        profileName.setMaxLength(32); skinUsername.setMaxLength(16); displayedName.setMaxLength(32); customTier.setMaxLength(24);
        if (loadedProfile != null) {
            profileName.setValue(loadedProfile.profileName());
            skinUsername.setValue(loadedProfile.skinSourceUsername());
            displayedName.setValue(loadedProfile.displayedName());
            customTier.setValue(loadedProfile.customTierText());
            tier = loadedProfile.tier();
        }
        addRenderableWidget(profileName); addRenderableWidget(skinUsername); addRenderableWidget(displayedName); addRenderableWidget(customTier);
        addRenderableWidget(Button.builder(Component.literal("Tier: " + tier.label()), b -> {
            tier = PvpTier.values()[(tier.ordinal() + 1) % PvpTier.values().length];
            b.setMessage(Component.literal("Tier: " + tier.label()));
        }).bounds(x + 18, y + 218, 112, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save Profile"), b -> save()).bounds(x + 138, y + 218, 112, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next Player"), b -> nextPlayer()).bounds(x + 258, y + 218, w - 276, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear Manual Target"), b -> {
            GlaxotionState.TARGETS.clearManualTarget(); status = "Manual target cleared; Auto Target is active.";
        }).bounds(x + 18, y + 244, 170, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(x + w - 108, y + 244, 90, 20).build());
    }

    private void save() {
        if (targetId == null) { status = "No other player is loaded within range."; return; }
        IdentityProfile profile = new IdentityProfile(
                loadedProfile == null ? UUID.randomUUID() : loadedProfile.id(),
                profileName.getValue(), skinUsername.getValue(), displayedName.getValue(), tier,
                customTier.getValue(), 0xFFFFFFFF, !displayedName.getValue().isBlank(), true
        );
        try {
            GlaxotionState.IDENTITIES.update(targetId, profile);
            loadedProfile = profile;
            status = "Profile saved locally. Skin lookup can be tested from the username field.";
            if (!skinUsername.getValue().isBlank()) {
                GlaxotionState.SKINS.resolve(skinUsername.getValue()).thenAccept(result ->
                        dev.glaxiblade.glaxotion.GlaxotionClient.LOGGER.info("Skin lookup completed: {}", result.isPresent() ? "resolved" : "not found"));
            }
        } catch (RuntimeException e) { status = "Could not save profile: " + e.getMessage(); }
    }

    private void nextPlayer() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        var players = minecraft.level.players().stream()
                .filter(p -> p != minecraft.player)
                .filter(p -> minecraft.player.distanceToSqr(p) <= GlaxotionState.TARGETS.range() * GlaxotionState.TARGETS.range())
                .toList();
        if (players.isEmpty()) { status = "No other loaded players within range."; return; }
        int index = -1;
        for (int i = 0; i < players.size(); i++) if (players.get(i).getUUID().equals(targetId)) index = i;
        var chosen = players.get((index + 1) % players.size());
        GlaxotionState.TARGETS.setManualTarget(chosen.getUUID());
        targetId = chosen.getUUID(); loadedProfile = GlaxotionState.IDENTITIES.activeProfile(targetId);
        status = "Selected " + chosen.getName().getString() + ". Reopen editor to load its fields.";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int w = Math.min(400, width - 24); int x = (width - w) / 2; int y = Math.max(24, (height - 270) / 2);
        graphics.fill(0, 0, width, height, 0xB9000000);
        graphics.fill(x, y, x + w, y + 272, 0xF20A111B);
        graphics.fill(x, y, x + w, y + 3, 0xFF32C5E8);
        graphics.text(font, "GLAXOTION", x + 18, y + 15, 0xFFFFFFFF, true);
        graphics.text(font, "LOCAL IDENTITY STUDIO", x + 18, y + 33, 0xFF74D4E8, false);
        graphics.text(font, "PROFILE NAME", x + 18, y + 43, 0xFF9CB7C8, false);
        graphics.text(font, "SKIN SOURCE USERNAME", x + 18, y + 87, 0xFF9CB7C8, false);
        graphics.text(font, "DISPLAYED IGN", x + 18, y + 131, 0xFF9CB7C8, false);
        graphics.text(font, "CUSTOM TIER LABEL", x + 18, y + 175, 0xFF9CB7C8, false);
        graphics.text(font, status, x + 18, y + 263, 0xFF9CB7C8, false);
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.gui.setScreen(null); }
}
