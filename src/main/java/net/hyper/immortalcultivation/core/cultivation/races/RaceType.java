package net.hyper.immortalcultivation.core.cultivation.races;

/**
 * Defines the available races in the cultivation world.
 * Each race provides specific bonuses and penalties to cultivation paths.
 */
public enum RaceType {
    HUMAN(1.0f, 1.0f, "Human", 0xFFE4B5,
        "Balanced in all aspects, humans possess limitless potential through their adaptability."),

    SPIRIT(1.2f, 0.8f, "Spirit", 0x87CEEB,
        "Born from spiritual essence, they excel at Qi cultivation but struggle with physical refinement."),

    DEMONIC_BEAST(0.8f, 1.2f, "Demonic Beast", 0xFF6347,
        "Powerful beasts with tremendous physical might, but limited spiritual affinity.");

    private final float qiMultiplier;
    private final float bodyMultiplier;
    private final String displayName;
    private final int color;
    private final String description;

    RaceType(float qiMultiplier, float bodyMultiplier, String displayName, int color, String description) {
        this.qiMultiplier = qiMultiplier;
        this.bodyMultiplier = bodyMultiplier;
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public float getQiMultiplier() { return qiMultiplier; }
    public float getBodyMultiplier() { return bodyMultiplier; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }
    public String getDescription() { return description; }

    /**
     * Check if this race has any cultivation penalties
     */
    public boolean hasPenalty() {
        return qiMultiplier < 1.0f || bodyMultiplier < 1.0f;
    }

    /**
     * Check if this race has any cultivation bonuses
     */
    public boolean hasBonus() {
        return qiMultiplier > 1.0f || bodyMultiplier > 1.0f;
    }

    /**
     * Get a formatted string describing the race's bonuses/penalties
     */
    public String getBonusDescription() {
        if (this == HUMAN) {
            return "No bonuses or penalties";
        }

        StringBuilder desc = new StringBuilder();

        if (qiMultiplier != 1.0f) {
            int percentage = Math.round((qiMultiplier - 1.0f) * 100);
            desc.append(percentage > 0 ? "+" : "").append(percentage).append("% Qi Cultivation");
        }

        if (bodyMultiplier != 1.0f) {
            if (desc.length() > 0) desc.append(", ");
            int percentage = Math.round((bodyMultiplier - 1.0f) * 100);
            desc.append(percentage > 0 ? "+" : "").append(percentage).append("% Body Cultivation");
        }

        return desc.toString();
    }
}