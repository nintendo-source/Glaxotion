package dev.glaxiblade.glaxotion.identity;

/**
 * Tier labels displayed by a local identity profile.
 */
public enum PvpTier {
    HT1("HT1"),
    LT1("LT1"),
    HT2("HT2"),
    LT2("LT2"),
    HT3("HT3"),
    LT3("LT3"),
    CUSTOM("Custom");

    private final String label;

    PvpTier(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
