package net.hyper.immortalcultivation.cultivation;

import net.minecraft.util.RandomSource;

public enum SpiritualRootType {
    MORTAL_ROOT(1.0f, "Mortal Root", 0x8B4513),
    STONE_ROOT(1.1f, "Stone Root", 0x696969),
    FLAME_ROOT(1.1f, "Flame Root", 0xFF4500),
    IRON_ROOT(1.1f, "Iron Root", 0xC0C0C0),
    WAVE_ROOT(1.1f, "Wave Root", 0x1E90FF),
    MAHOGANY_ROOT(1.1f, "Mahogany Root", 0xA0522D),
    BREEZE_ROOT(1.1f, "Breeze Root", 0x87CEEB),
    SPARK_ROOT(1.1f, "Spark Root", 0xFFFF00),
    TOXIC_ROOT(1.1f, "Toxic Root", 0x9932CC),
    DARK_MOUNTAIN_ROOT(1.2f, "Dark Mountain Root", 0x2F4F4F),
    PHOENIX_ROOT(1.2f, "Phoenix Root", 0xFF6347),
    CELESTIAL_STEEL_ROOT(1.2f, "Celestial Steel Root", 0xE6E6FA),
    ABYSSAL_MOON_ROOT(1.2f, "Abyssal Moon Root", 0x191970),
    MYRIAD_BRANCHES_ROOT(1.2f, "Myriad Branches Root", 0x228B22),
    GALE_ROOT(1.2f, "Gale Root", 0xF0F8FF),
    THUNDER_ROOT(1.2f, "Thunder Root", 0x9370DB),
    THOUSAND_VENOMS_ROOT(1.2f, "Thousand Venoms Root", 0x8B008B),
    OMEN_ROOT(1.2f, "Omen Root", 0x4B0082),
    NETHER_ROOT(1.2f, "Nether Root", 0x8B0000),
    LIGHT_ROOT(1.2f, "Light Root", 0xFFFACD),
    SHADOW_ROOT(1.2f, "Shadow Root", 0x36454F),
    VOID_ROOT(1.3f, "Void Root", 0x0A0A0A),
    TEMPORAL_ROOT(1.3f, "Temporal Root", 0x4682B4),
    WUXING_ROOT(1.3f, "Wuxing Root", 0xDAA520),
    DIVINE_TEMPEST_ROOT(1.3f, "Divine Tempest Root", 0x483D8B),
    ECLIPSING_ROOT(1.3f, "Eclipsing Root", 0x2F2F2F),
    PRIMORDIAL_CHAOS_ROOT(1.4f, "Primordial Chaos Root", 0x301934),
    SAMSARA_ROOT(1.4f, "Samsara Root", 0xFFE4B5),
    KARMIC_RETRIBUTION_ROOT(1.4f, "Karmic Retribution Root", 0x722F37),
    YIN_MONARCH_ROOT(1.4f, "Yin Monarch Root", 0x1C1C1C),
    HEAVENLY_PUNISHMENT_ROOT(1.4f, "Heavenly Punishment Root", 0xF8F8FF),
    HEAVENLY_ROOT(1.5f, "Heavenly Root", 0xFFD700);

    private final float cultivationMultiplier;
    private final String displayName;
    private final int color;

    SpiritualRootType(float multiplier, String name, int color) {
        this.cultivationMultiplier = multiplier;
        this.displayName = name;
        this.color = color;
    }

    public float getCultivationMultiplier() { return cultivationMultiplier; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }

    // Weighted random generation
    public static SpiritualRootType generateRandom(RandomSource random) {
        float roll = random.nextFloat();

        // Mortal Root - Most common (50% chance)
        if (roll < 0.5f) return MORTAL_ROOT;

        // 1.1f multiplier roots - Common (30% total, 3.75% each)
        if (roll < 0.5375f) return STONE_ROOT;          // 3.75%
        if (roll < 0.575f) return FLAME_ROOT;           // 3.75%
        if (roll < 0.6125f) return IRON_ROOT;           // 3.75%
        if (roll < 0.65f) return WAVE_ROOT;             // 3.75%
        if (roll < 0.6875f) return MAHOGANY_ROOT;       // 3.75%
        if (roll < 0.725f) return BREEZE_ROOT;          // 3.75%
        if (roll < 0.7625f) return SPARK_ROOT;          // 3.75%
        if (roll < 0.8f) return TOXIC_ROOT;             // 3.75%

        // 1.2f multiplier roots - Uncommon (15% total, 1.25% each)
        if (roll < 0.8125f) return DARK_MOUNTAIN_ROOT;     // 1.25%
        if (roll < 0.825f) return PHOENIX_ROOT;            // 1.25%
        if (roll < 0.8375f) return CELESTIAL_STEEL_ROOT;   // 1.25%
        if (roll < 0.85f) return ABYSSAL_MOON_ROOT;        // 1.25%
        if (roll < 0.8625f) return MYRIAD_BRANCHES_ROOT;   // 1.25%
        if (roll < 0.875f) return GALE_ROOT;               // 1.25%
        if (roll < 0.8875f) return THUNDER_ROOT;           // 1.25%
        if (roll < 0.9f) return THOUSAND_VENOMS_ROOT;      // 1.25%
        if (roll < 0.9125f) return OMEN_ROOT;              // 1.25%
        if (roll < 0.925f) return NETHER_ROOT;             // 1.25%
        if (roll < 0.9375f) return LIGHT_ROOT;             // 1.25%
        if (roll < 0.95f) return SHADOW_ROOT;              // 1.25%

        // 1.3f multiplier roots - Rare (3% total, 0.6% each)
        if (roll < 0.956f) return VOID_ROOT;               // 0.6%
        if (roll < 0.962f) return TEMPORAL_ROOT;           // 0.6%
        if (roll < 0.968f) return WUXING_ROOT;             // 0.6%
        if (roll < 0.974f) return DIVINE_TEMPEST_ROOT;     // 0.6%
        if (roll < 0.98f) return ECLIPSING_ROOT;           // 0.6%

        // 1.4f multiplier roots - Legendary (1.9% total, 0.38% each)
        if (roll < 0.9838f) return PRIMORDIAL_CHAOS_ROOT;     // 0.38%
        if (roll < 0.9876f) return SAMSARA_ROOT;              // 0.38%
        if (roll < 0.9914f) return KARMIC_RETRIBUTION_ROOT;   // 0.38%
        if (roll < 0.9952f) return YIN_MONARCH_ROOT;          // 0.38%
        if (roll < 0.999f) return HEAVENLY_PUNISHMENT_ROOT;   // 0.38%

        // Heavenly Root - Most rare (0.1% chance)
        return HEAVENLY_ROOT;                                  // 0.1%
    }
}
