package dev.glaxiblade.glaxotion.ui;

import dev.glaxiblade.glaxotion.GlaxotionState;
import dev.glaxiblade.glaxotion.identity.IdentityProfile;
import dev.glaxiblade.glaxotion.identity.PvpTier;
import dev.glaxiblade.glaxotion.render.IdentityRenderHooks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

/** Two-page, fixed-layout editor. No clipped controls or hidden scroll area. */
public final class GlaxotionScreen extends Screen {
    private EditBox profileName, skinUsername, displayedName, customTier;
    private UUID targetId;
    private IdentityProfile loadedProfile;
    private PvpTier tier = PvpTier.HT3;
    private int selectedColor = 0xFFFFFFFF;
    private int colorIndex;
    private int page;
    private String status = "Auto Target uses the nearest loaded player.";
    private static final int[] COLORS = {0xFFFFFFFF, 0xFF35D6FF, 0xFFFFD34E, 0xFFFF5D73, 0xFF8BFF9B, 0xFFB78CFF};
    private static final double[] RANGES = {16, 24, 32, 48, 64, 96};
    private int panelX, panelY, panelWidth, panelHeight;

    public GlaxotionScreen() { super(Component.literal("Glaxotion")); }

    @Override
    protected void init() {
        clearWidgets();
        int w = Math.max(1, Math.min(420, width - 24));
        int h = Math.max(1, Math.min(350, height - 20));
        panelWidth = w; panelHeight = h;
        panelX = (width - w) / 2; panelY = (height - h) / 2;
        targetId = GlaxotionState.TARGETS.currentTarget().orElse(null);
        loadedProfile = targetId == null ? null : GlaxotionState.IDENTITIES.activeProfile(targetId);
        if (page == 0) buildIdentityPage();
        else buildTargetPage();
    }

    private void buildIdentityPage() {
        int x = panelX, y = panelY, w = panelWidth;
        if (w < 360 || panelHeight < 330) { status = "Window too small. Enlarge Minecraft to edit profiles."; return; }
        profileName = new EditBox(font, x + 18, y + 72, w - 36, 20, Component.literal("Profile name"));
        skinUsername = new EditBox(font, x + 18, y + 116, w - 36, 20, Component.literal("Skin source Java username"));
        displayedName = new EditBox(font, x + 18, y + 160, w - 36, 20, Component.literal("Displayed IGN"));
        customTier = new EditBox(font, x + 18, y + 204, w - 36, 20, Component.literal("Custom tier label"));
        profileName.setMaxLength(32); skinUsername.setMaxLength(16);
        displayedName.setMaxLength(32); customTier.setMaxLength(24);
        if (loadedProfile != null) loadFields(loadedProfile);
        else {
            profileName.setValue("Default");
            skinUsername.setValue("");
            displayedName.setValue("");
            customTier.setValue("");
        }
        addRenderableWidget(profileName);
        addRenderableWidget(skinUsername);
        addRenderableWidget(displayedName);
        addRenderableWidget(customTier);
        addRenderableWidget(Button.builder(Component.literal("Tier: " + tier.label()), b -> {
            tier = PvpTier.values()[(tier.ordinal() + 1) % PvpTier.values().length];
            b.setMessage(Component.literal("Tier: " + tier.label()));
        }).bounds(x + 18, y + 242, 112, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Save Profile"), b -> save())
                .bounds(x + 138, y + 242, 112, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Target & Presets  →"), b -> {
            save(); page = 1; rebuild();
        }).bounds(x + 258, y + 242, w - 276, 22).build());
    }

    private void buildTargetPage() {
        int x = panelX, y = panelY, w = panelWidth;
        if (w < 360 || panelHeight < 300) { status = "Window too small. Enlarge Minecraft to manage targets."; return; }
        addRenderableWidget(Button.builder(Component.literal("←  Identity Editor"), b -> { page = 0; rebuild(); })
                .bounds(x + 18, y + 72, w - 36, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Next Player"), b -> { nextPlayer(); rebuild(); })
                .bounds(x + 18, y + 108, 180, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Auto Target"), b -> {
            GlaxotionState.TARGETS.clearManualTarget();
            GlaxotionState.TARGETS.tick(minecraft);
            targetId = GlaxotionState.TARGETS.currentTarget().orElse(null);
            loadedProfile = targetId == null ? null : GlaxotionState.IDENTITIES.activeProfile(targetId);
            status = "Manual selection cleared; nearest player will be used.";
        }).bounds(x + 208, y + 108, w - 226, 22).build());
        addRenderableWidget(Button.builder(Component.literal("New Preset"), b -> newPreset())
                .bounds(x + 18, y + 146, 112, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Next Preset"), b -> nextPreset())
                .bounds(x + 138, y + 146, 112, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Delete Preset"), b -> deletePreset())
                .bounds(x + 258, y + 146, w - 276, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Color: " + colorName()), b -> {
            colorIndex = (colorIndex + 1) % COLORS.length;
            selectedColor = COLORS[colorIndex];
            if (targetId != null && loadedProfile != null) {
                loadedProfile = new IdentityProfile(loadedProfile.id(), loadedProfile.profileName(),
                        loadedProfile.skinSourceUsername(), loadedProfile.displayedName(), loadedProfile.tier(),
                        loadedProfile.customTierText(), selectedColor, loadedProfile.showDisplayedName(), loadedProfile.showTier());
                GlaxotionState.IDENTITIES.update(targetId, loadedProfile);
                IdentityRenderHooks.refresh(targetId);
            }
            b.setMessage(Component.literal("Color: " + colorName()));
        }).bounds(x + 18, y + 190, 180, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Range: " + (int) GlaxotionState.TARGETS.range() + " blocks"), b -> {
            int current = 0;
            for (int i = 0; i < RANGES.length; i++) if (RANGES[i] == GlaxotionState.TARGETS.range()) current = i;
            double next = RANGES[(current + 1) % RANGES.length];
            GlaxotionState.TARGETS.setRange(next);
            b.setMessage(Component.literal("Range: " + (int) next + " blocks"));
        }).bounds(x + 208, y + 190, w - 226, 22).build());
    }

    private void rebuild() {
        clearWidgets();
        if (page == 0) buildIdentityPage();
        else buildTargetPage();
    }

    private void loadFields(IdentityProfile profile) {
        profileName.setValue(profile.profileName());
        skinUsername.setValue(profile.skinSourceUsername());
        displayedName.setValue(profile.displayedName());
        customTier.setValue(profile.customTierText());
        tier = profile.tier();
        selectedColor = profile.textColor();
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == selectedColor) colorIndex = i;
    }

    private String colorName() {
        return switch (selectedColor) {
            case 0xFF35D6FF -> "Cyan";
            case 0xFFFFD34E -> "Gold";
            case 0xFFFF5D73 -> "Coral";
            case 0xFF8BFF9B -> "Mint";
            case 0xFFB78CFF -> "Violet";
            default -> "White";
        };
    }

    private void save() {
        if (targetId == null) {
            status = "No player target. Open Target & Presets and select another loaded player first.";
            return;
        }
        IdentityProfile profile = new IdentityProfile(
                loadedProfile == null ? UUID.randomUUID() : loadedProfile.id(),
                profileName.getValue(), skinUsername.getValue(), displayedName.getValue(), tier,
                customTier.getValue(), selectedColor, !displayedName.getValue().isBlank(), true
        );
        try {
            GlaxotionState.IDENTITIES.update(targetId, profile);
            loadedProfile = profile;
            IdentityRenderHooks.refresh(targetId);
            status = "Saved for " + targetId.toString().substring(0, 8) + ". Skin: " + (profile.skinSourceUsername().isBlank() ? "unchanged" : "lookup started");
        } catch (RuntimeException e) {
            status = "Save failed: " + e.getMessage();
        }
    }

    private void newPreset() {
        loadedProfile = null;
        status = "New preset selected. Return to Identity Editor to enter its values and save.";
        page = 0;
        rebuild();
        profileName.setValue("Preset " + (targetId == null ? 1 : GlaxotionState.IDENTITIES.profiles(targetId).size() + 1));
        skinUsername.setValue("");
        displayedName.setValue("");
        customTier.setValue("");
        tier = PvpTier.HT3;
    }

    private void nextPreset() {
        if (targetId == null) { status = "Select a player first."; return; }
        var profiles = GlaxotionState.IDENTITIES.profiles(targetId);
        if (profiles.isEmpty()) { status = "No saved presets for this player."; return; }
        int index = -1;
        if (loadedProfile != null) for (int i = 0; i < profiles.size(); i++)
            if (profiles.get(i).id().equals(loadedProfile.id())) index = i;
        loadedProfile = profiles.get((index + 1) % profiles.size());
        GlaxotionState.IDENTITIES.select(targetId, loadedProfile.id());
        page = 0; rebuild();
        loadFields(loadedProfile);
        status = "Loaded preset: " + loadedProfile.profileName();
    }

    private void deletePreset() {
        if (targetId == null || loadedProfile == null) { status = "Choose a saved preset first."; return; }
        GlaxotionState.IDENTITIES.remove(targetId, loadedProfile.id());
        loadedProfile = GlaxotionState.IDENTITIES.activeProfile(targetId);
        status = "Preset deleted.";
    }

    private void nextPlayer() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            status = "Join a world first.";
            return;
        }
        var players = minecraft.level.players().stream()
                .filter(p -> p != minecraft.player)
                .filter(p -> minecraft.player.distanceToSqr(p) <= GlaxotionState.TARGETS.range() * GlaxotionState.TARGETS.range())
                .toList();
        if (players.isEmpty()) { status = "No other loaded players within " + (int) GlaxotionState.TARGETS.range() + " blocks."; return; }
        int index = -1;
        for (int i = 0; i < players.size(); i++) if (players.get(i).getUUID().equals(targetId)) index = i;
        var chosen = players.get((index + 1) % players.size());
        GlaxotionState.TARGETS.setManualTarget(chosen.getUUID());
        targetId = chosen.getUUID();
        loadedProfile = GlaxotionState.IDENTITIES.activeProfile(targetId);
        status = "Selected " + chosen.getName().getString() + ".";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = panelX, y = panelY, w = panelWidth, h = panelHeight;
        graphics.fill(0, 0, width, height, 0xB9000000);
        graphics.fill(x, y, x + w, y + h, 0xF20A111B);
        graphics.fill(x, y, x + w, y + 3, 0xFF32C5E8);
        graphics.text(font, "GLAXOTION", x + 18, y + 15, 0xFFFFFFFF, true);
        graphics.text(font, page == 0 ? "IDENTITY EDITOR  •  1 / 2" : "TARGET & PRESETS  •  2 / 2", x + 18, y + 36, 0xFF74D4E8, false);
        if (page == 0) {
            graphics.text(font, "PROFILE NAME", x + 18, y + 60, 0xFF9CB7C8, false);
            graphics.text(font, "SKIN SOURCE JAVA USERNAME", x + 18, y + 104, 0xFF9CB7C8, false);
            graphics.text(font, "DISPLAYED IGN", x + 18, y + 148, 0xFF9CB7C8, false);
            graphics.text(font, "CUSTOM TIER LABEL", x + 18, y + 192, 0xFF9CB7C8, false);
            if (targetId != null) graphics.text(font, IdentityRenderHooks.skinStatus(targetId), x + 18, y + 272, 0xFF74D4E8, false);
        } else {
            String target = targetId == null ? "No target selected" : "Target: " + targetId.toString().substring(0, 8);
            graphics.text(font, target, x + 18, y + 56, 0xFFFFFFFF, false);
            graphics.text(font, "Select a loaded player, then edit and save their local profile.", x + 18, y + 230, 0xFF9CB7C8, false);
        }
        graphics.fill(x + 12, y + h - 45, x + w - 12, y + h - 44, 0xFF30404D);
        graphics.text(font, status, x + 18, y + h - 32, 0xFF74D4E8, false);
        graphics.text(font, "ESC  Close", x + w - 82, y + h - 16, 0xFF9CB7C8, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }
}
