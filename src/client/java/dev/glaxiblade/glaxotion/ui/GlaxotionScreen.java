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

/** Responsive two-page identity editor. All controls are placed inside the usable GUI area. */
public final class GlaxotionScreen extends Screen {
    private EditBox profileName, skinUsername, displayedName, customTier;
    private UUID targetId;
    private IdentityProfile loadedProfile;
    private PvpTier tier = PvpTier.HT3;
    private int selectedColor = 0xFFFFFFFF;
    private int colorIndex;
    private int page;
    private String status = "Select a nearby player to begin.";
    private static final int[] COLORS = {0xFFFFFFFF, 0xFF35D6FF, 0xFFFFD34E, 0xFFFF5D73, 0xFF8BFF9B, 0xFFB78CFF};
    private static final double[] RANGES = {16, 24, 32, 48, 64, 96};
    private int panelX, panelY, panelWidth, panelHeight;

    public GlaxotionScreen() { super(Component.literal("Glaxotion")); }

    @Override
    protected void init() {
        clearWidgets();
        // Minecraft GUI dimensions are scaled logical pixels, not desktop pixels.
        // Keep the panel compact enough for high GUI-scale settings.
        panelWidth = Math.max(240, Math.min(420, width - 16));
        panelHeight = Math.max(220, Math.min(350, height - 16));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        targetId = GlaxotionState.TARGETS.currentTarget().orElse(null);
        loadedProfile = targetId == null ? null : GlaxotionState.IDENTITIES.activeProfile(targetId);
        if (panelWidth < 240 || panelHeight < 220) {
            status = "Increase Minecraft's GUI space to use Glaxotion.";
            return;
        }
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        if (page == 0) buildIdentityPage();
        else buildTargetPage();
    }

    private void buildIdentityPage() {
        int x = panelX, y = panelY, w = panelWidth, h = panelHeight;
        int fieldStep = Math.max(25, Math.min(39, (h - 112) / 4));
        int fieldStart = 43;
        int inputWidth = w - 32;

        profileName = new EditBox(font, x + 16, y + fieldStart + 9, inputWidth, 18, Component.literal("Profile name"));
        skinUsername = new EditBox(font, x + 16, y + fieldStart + fieldStep + 9, inputWidth, 18, Component.literal("Skin username"));
        displayedName = new EditBox(font, x + 16, y + fieldStart + fieldStep * 2 + 9, inputWidth, 18, Component.literal("Displayed IGN"));
        customTier = new EditBox(font, x + 16, y + fieldStart + fieldStep * 3 + 9, inputWidth, 18, Component.literal("Custom tier"));
        profileName.setMaxLength(32);
        skinUsername.setMaxLength(16);
        displayedName.setMaxLength(32);
        customTier.setMaxLength(24);

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

        int buttonY = y + h - 49;
        int gap = 5;
        int buttonWidth = Math.max(54, (w - 32 - gap * 2) / 3);
        addRenderableWidget(Button.builder(Component.literal("Tier"), b -> {
            tier = PvpTier.values()[(tier.ordinal() + 1) % PvpTier.values().length];
            status = "Tier set to " + tier.label() + ". Save to apply.";
        }).bounds(x + 16, buttonY, buttonWidth, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(x + 16 + buttonWidth + gap, buttonY, buttonWidth, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Targets →"), b -> {
            page = 1;
            rebuild();
        }).bounds(x + 16 + (buttonWidth + gap) * 2, buttonY, buttonWidth, 22).build());
    }

    private void buildTargetPage() {
        int x = panelX, y = panelY, w = panelWidth;
        int inner = w - 32;
        int gap = 6;
        int half = (inner - gap) / 2;
        int third = (inner - gap * 2) / 3;

        addRenderableWidget(Button.builder(Component.literal("← Back to editor"), b -> {
            page = 0;
            rebuild();
        }).bounds(x + 16, y + 49, inner, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next Player"), b -> {
            nextPlayer();
            rebuild();
        }).bounds(x + 16, y + 91, half, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Auto Target"), b -> {
            GlaxotionState.TARGETS.clearManualTarget();
            GlaxotionState.TARGETS.tick(minecraft);
            targetId = GlaxotionState.TARGETS.currentTarget().orElse(null);
            loadedProfile = targetId == null ? null : GlaxotionState.IDENTITIES.activeProfile(targetId);
            status = "Automatic targeting enabled.";
            rebuild();
        }).bounds(x + 16 + half + gap, y + 91, half, 22).build());

        addRenderableWidget(Button.builder(Component.literal("New"), b -> newPreset())
                .bounds(x + 16, y + 121, third, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Next"), b -> nextPreset())
                .bounds(x + 16 + third + gap, y + 121, third, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deletePreset())
                .bounds(x + 16 + (third + gap) * 2, y + 121, third, 22).build());

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
            status = "Name color updated.";
        }).bounds(x + 16, y + 151, half, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Range: " + (int) GlaxotionState.TARGETS.range()), b -> {
            int current = 0;
            for (int i = 0; i < RANGES.length; i++) if (RANGES[i] == GlaxotionState.TARGETS.range()) current = i;
            double next = RANGES[(current + 1) % RANGES.length];
            GlaxotionState.TARGETS.setRange(next);
            b.setMessage(Component.literal("Range: " + (int) next));
            status = "Target range set to " + (int) next + " blocks.";
        }).bounds(x + 16 + half + gap, y + 151, half, 22).build());
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
            status = "No target. Open Targets and select a nearby player first.";
            return;
        }
        // If the user only enters a skin username, use it as the displayed
        // local name too. An explicitly entered Displayed IGN still takes priority.
        String shownName = displayedName.getValue().isBlank()
                ? skinUsername.getValue().trim()
                : displayedName.getValue().trim();
        IdentityProfile profile = new IdentityProfile(
                loadedProfile == null ? UUID.randomUUID() : loadedProfile.id(),
                profileName.getValue(), skinUsername.getValue(), shownName, tier,
                customTier.getValue(), selectedColor, !shownName.isBlank(), true
        );
        try {
            GlaxotionState.IDENTITIES.update(targetId, profile);
            loadedProfile = profile;
            IdentityRenderHooks.refresh(targetId);
            status = "Profile saved. Skin lookup starts in the background.";
        } catch (RuntimeException e) {
            status = "Save failed: " + e.getClass().getSimpleName();
        }
    }

    private void newPreset() {
        loadedProfile = null;
        page = 0;
        rebuild();
        profileName.setValue("Preset " + (targetId == null ? 1 : GlaxotionState.IDENTITIES.profiles(targetId).size() + 1));
        skinUsername.setValue("");
        displayedName.setValue("");
        customTier.setValue("");
        tier = PvpTier.HT3;
        status = "New preset ready. Edit it and press Save.";
    }

    private void nextPreset() {
        if (targetId == null) { status = "Select a player first."; return; }
        var profiles = GlaxotionState.IDENTITIES.profiles(targetId);
        if (profiles.isEmpty()) { status = "No saved presets yet."; return; }
        int index = -1;
        if (loadedProfile != null) for (int i = 0; i < profiles.size(); i++)
            if (profiles.get(i).id().equals(loadedProfile.id())) index = i;
        loadedProfile = profiles.get((index + 1) % profiles.size());
        GlaxotionState.IDENTITIES.select(targetId, loadedProfile.id());
        page = 0;
        rebuild();
        loadFields(loadedProfile);
        status = "Loaded " + loadedProfile.profileName() + ".";
    }

    private void deletePreset() {
        if (targetId == null || loadedProfile == null) { status = "Choose a saved preset first."; return; }
        GlaxotionState.IDENTITIES.remove(targetId, loadedProfile.id());
        loadedProfile = GlaxotionState.IDENTITIES.activeProfile(targetId);
        status = "Preset deleted.";
        page = 0;
        rebuild();
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
        if (players.isEmpty()) {
            status = "No other loaded players in range.";
            return;
        }
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
        graphics.fill(0, 0, width, height, 0xB9000000);
        int x = panelX, y = panelY, w = panelWidth, h = panelHeight;
        graphics.fill(x, y, x + w, y + h, 0xF20A111B);
        graphics.fill(x, y, x + w, y + 3, 0xFF32C5E8);
        graphics.text(font, "GLAXOTION", x + 16, y + 12, 0xFFFFFFFF, true);
        graphics.text(font, page == 0 ? "IDENTITY STUDIO" : "TARGETS & PRESETS", x + 16, y + 29, 0xFF74D4E8, false);

        if (page == 0) {
            int fieldStep = Math.max(25, Math.min(39, (h - 112) / 4));
            int start = 43;
            graphics.text(font, "PROFILE NAME", x + 16, y + start, 0xFF9CB7C8, false);
            graphics.text(font, "SKIN SOURCE (JAVA USERNAME)", x + 16, y + start + fieldStep, 0xFF9CB7C8, false);
            graphics.text(font, "DISPLAYED IGN", x + 16, y + start + fieldStep * 2, 0xFF9CB7C8, false);
            graphics.text(font, "CUSTOM TIER LABEL", x + 16, y + start + fieldStep * 3, 0xFF9CB7C8, false);
            if (targetId != null) graphics.text(font, IdentityRenderHooks.skinStatus(targetId), x + 16, y + h - 65, 0xFF74D4E8, false);
        } else {
            String target = targetId == null ? "No target selected" : "Target ID: " + targetId.toString().substring(0, 8);
            graphics.text(font, target, x + 16, y + 76, 0xFFFFFFFF, false);
        }

        graphics.fill(x + 12, y + h - 31, x + w - 12, y + h - 30, 0xFF30404D);
        String shortStatus = status.length() > Math.max(18, (w - 32) / 5) ? status.substring(0, Math.max(15, (w - 44) / 5)) + "…" : status;
        graphics.text(font, shortStatus, x + 16, y + h - 23, 0xFF74D4E8, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }
}
