package net.hyper.immortalcultivation.core.cultivation.talents;

import net.minecraft.util.RandomSource;

public enum PhysiqueType {
    MORTAL_PHYSIQUE(1.0f, "Mortal Physique", 0x8B4513),
    STONE_PHYSIQUE(1.1f, "Stone Physique", 0x696969),
    FLAME_PHYSIQUE(1.1f, "Flame Physique", 0xFF4500),
    IRON_PHYSIQUE(1.1f, "Iron Physique", 0xC0C0C0),
    WAVE_PHYSIQUE(1.1f, "Wave Physique", 0x1E90FF),
    MAHOGANY_PHYSIQUE(1.1f, "Mahogany Physique", 0xA0522D),
    BREEZE_PHYSIQUE(1.1f, "Breeze Physique", 0x87CEEB),
    SPARK_PHYSIQUE(1.1f, "Spark Physique", 0xFFFF00),
    TOXIC_PHYSIQUE(1.1f, "Toxic Physique", 0x9932CC),
    DARK_MOUNTAIN_PHYSIQUE(1.2f, "Dark Mountain Physique", 0x2F4F4F),
    PHOENIX_PHYSIQUE(1.2f, "Phoenix Physique", 0xFF6347),
    CELESTIAL_STEEL_PHYSIQUE(1.2f, "Celestial Steel Physique", 0xE6E6FA),
    ABYSSAL_MOON_PHYSIQUE(1.2f, "Abyssal Moon Physique", 0x191970),
    MYRIAD_BRANCHES_PHYSIQUE(1.2f, "Myriad Branches Physique", 0x228B22),
    GALE_PHYSIQUE(1.2f, "Gale Physique", 0xF0F8FF),
    THUNDER_PHYSIQUE(1.2f, "Thunder Physique", 0x9370DB),
    THOUSAND_VENOMS_PHYSIQUE(1.2f, "Thousand Venoms Physique", 0x8B008B),
    OMEN_PHYSIQUE(1.2f, "Omen Physique", 0x4B0082),
    NETHER_PHYSIQUE(1.2f, "Nether Physique", 0x8B0000),
    LIGHT_PHYSIQUE(1.2f, "Light Physique", 0xFFFACD),
    SHADOW_PHYSIQUE(1.2f, "Shadow Physique", 0x36454F),
    VOID_PHYSIQUE(1.3f, "Void Physique", 0x0A0A0A),
    TEMPORAL_PHYSIQUE(1.3f, "Temporal Physique", 0x4682B4),
    WUXING_PHYSIQUE(1.3f, "Wuxing Physique", 0xDAA520),
    DIVINE_TEMPEST_PHYSIQUE(1.3f, "Divine Tempest Physique", 0x483D8B),
    ECLIPSING_PHYSIQUE(1.3f, "Eclipsing Physique", 0x2F2F2F),
    PRIMORDIAL_CHAOS_PHYSIQUE(1.4f, "Primordial Chaos Physique", 0x301934),
    SAMSARA_PHYSIQUE(1.4f, "Samsara Physique", 0xFFE4B5),
    KARMIC_RETRIBUTION_PHYSIQUE(1.4f, "Karmic Retribution Physique", 0x722F37),
    YIN_MONARCH_PHYSIQUE(1.4f, "Yin Monarch Physique", 0x1C1C1C),
    HEAVENLY_PUNISHMENT_PHYSIQUE(1.4f, "Heavenly Punishment Physique", 0xF8F8FF),
    HEAVENLY_PHYSIQUE(1.5f, "Heavenly Physique", 0xFFD700);

    private final float cultivationMultiplier;
    private final String displayName;
    private final int color;

    PhysiqueType(float multiplier, String name, int color) {
        this.cultivationMultiplier = multiplier;
        this.displayName = name;
        this.color = color;
    }

    public float getCultivationMultiplier() { return cultivationMultiplier; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }

    // Weighted random generation
    public static PhysiqueType generateRandom(RandomSource random) {
        float roll = random.nextFloat();

        // Mortal Physique - Most common (50% chance)
        if (roll < 0.5f) return MORTAL_PHYSIQUE;

        // 1.1f multiplier roots - Common (30% total, 3.75% each)
        if (roll < 0.5375f) return STONE_PHYSIQUE;          // 3.75%
        if (roll < 0.575f) return FLAME_PHYSIQUE;           // 3.75%
        if (roll < 0.6125f) return IRON_PHYSIQUE;           // 3.75%
        if (roll < 0.65f) return WAVE_PHYSIQUE;             // 3.75%
        if (roll < 0.6875f) return MAHOGANY_PHYSIQUE;       // 3.75%
        if (roll < 0.725f) return BREEZE_PHYSIQUE;          // 3.75%
        if (roll < 0.7625f) return SPARK_PHYSIQUE;          // 3.75%
        if (roll < 0.8f) return TOXIC_PHYSIQUE;             // 3.75%

        // 1.2f multiplier roots - Uncommon (15% total, 1.25% each)
        if (roll < 0.8125f) return DARK_MOUNTAIN_PHYSIQUE;     // 1.25%
        if (roll < 0.825f) return PHOENIX_PHYSIQUE;            // 1.25%
        if (roll < 0.8375f) return CELESTIAL_STEEL_PHYSIQUE;   // 1.25%
        if (roll < 0.85f) return ABYSSAL_MOON_PHYSIQUE;        // 1.25%
        if (roll < 0.8625f) return MYRIAD_BRANCHES_PHYSIQUE;   // 1.25%
        if (roll < 0.875f) return GALE_PHYSIQUE;               // 1.25%
        if (roll < 0.8875f) return THUNDER_PHYSIQUE;           // 1.25%
        if (roll < 0.9f) return THOUSAND_VENOMS_PHYSIQUE;      // 1.25%
        if (roll < 0.9125f) return OMEN_PHYSIQUE;              // 1.25%
        if (roll < 0.925f) return NETHER_PHYSIQUE;             // 1.25%
        if (roll < 0.9375f) return LIGHT_PHYSIQUE;             // 1.25%
        if (roll < 0.95f) return SHADOW_PHYSIQUE;              // 1.25%

        // 1.3f multiplier roots - Rare (3% total, 0.6% each)
        if (roll < 0.956f) return VOID_PHYSIQUE;               // 0.6%
        if (roll < 0.962f) return TEMPORAL_PHYSIQUE;           // 0.6%
        if (roll < 0.968f) return WUXING_PHYSIQUE;             // 0.6%
        if (roll < 0.974f) return DIVINE_TEMPEST_PHYSIQUE;     // 0.6%
        if (roll < 0.98f) return ECLIPSING_PHYSIQUE;           // 0.6%

        // 1.4f multiplier roots - Legendary (1.9% total, 0.38% each)
        if (roll < 0.9838f) return PRIMORDIAL_CHAOS_PHYSIQUE;     // 0.38%
        if (roll < 0.9876f) return SAMSARA_PHYSIQUE;              // 0.38%
        if (roll < 0.9914f) return KARMIC_RETRIBUTION_PHYSIQUE;   // 0.38%
        if (roll < 0.9952f) return YIN_MONARCH_PHYSIQUE;          // 0.38%
        if (roll < 0.999f) return HEAVENLY_PUNISHMENT_PHYSIQUE;   // 0.38%

        // Heavenly Physique - Most rare (0.1% chance)
        return HEAVENLY_PHYSIQUE;                                  // 0.1%
    }
}
