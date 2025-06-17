package net.hyper.immortalcultivation.cultivation;

import net.minecraft.util.RandomSource;

public enum SpiritualRootType {
    MORTAL_ROOT(1.0f, "Mortal Root", 0x8B4513),
    STONE_ROOT(1.1f, "Stone Root", 0x32CD32),
    FLAME_ROOT(1.1f, "Flame Root", 0x4169E1),
    IRON_ROOT(1.1f, "Iron Root", 0xFFD700),
    WAVE_ROOT(1.1f, "Wave Root", 0xFFD700),
    MAHOGANY_ROOT(1.1f, "Mahogany Root", 0xFFD700),
    BREEZE_ROOT(1.1f, "Breeze Root", 0xFFD700),
    SPARK_ROOT(1.1f, "Spark Root", 0xFFD700),
    TOXIC_ROOT(1.1f, "Toxic Root", 0xFFD700),
    DARK_MOUNTAIN_ROOT(1.2f, "Dark Mountain Root", 0x32CD32),
    PHOENIX_ROOT(1.2f, "Phoenix Root", 0x4169E1),
    CELESTIAL_STEEL_ROOT(1.2f, "Celestial Steel Root", 0xFFD700),
    ABYSSAL_MOON_ROOT(1.2f, "Abyssal Moon Root", 0xFFD700),
    MYRIAD_BRANCHES_ROOT(1.2f, "Myriad Branches Root", 0xFFD700),
    GALE_ROOT(1.2f, "Gale Root", 0xFFD700),
    THUNDER_ROOT(1.2f, "Thunder Root", 0xFFD700),
    THOUSAND_VENOMS_ROOT(1.2f, "Thousand Venoms Root", 0xFFD700),
    OMEN_ROOT(1.2f, "Omen Root", 0xFFD700),
    NETHER_ROOT(1.2f, "Nether Root", 0xFFD700),
    LIGHT_ROOT(1.2f, "Light Root", 0xFFD700),
    SHADOW_ROOT(1.2f, "Shadow Root", 0xFFD700),
    VOID_ROOT(1.3f, "Void Root", 0xFFD700),
    TEMPORAL_ROOT(1.3f, "Temporal Root", 0xFFD700),
    WUXING_ROOT(1.4f, "Wuxing Root", 0xFFD700),
    PRIMORDIAL_CHAOS_ROOT(1.4f, "Primordial Chaos Root", 0xFFD700),
    HEAVENLY_ROOT(1.5f, "Heavenly Root", 0xFFD700),
    ECLIPSING_ROOT(1.4f, "Eclipsing Root", 0xFFD700),
    SAMSARA_ROOT(1.4f, "Samsara Root", 0xFFD700),
    KARMIC_RETRIBUTION_ROOT(1.4f, "Karmic Retribution Root", 0xFFD700),
    YIN_MONARCH_ROOT(1.4f, "Yin Monarch Root", 0xFFD700),
    DIVINE_TEMPEST_ROOT(1.4f, "Divine Tempest Root", 0xFFD700),
    HEAVENLY_PUNISHMENT_ROOT(1.4f, "Heavenly Punishment Root", 0xFFD700);

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

        // Legendary tier
        if (roll < 0.001f) return HEAVENLY_ROOT;          // 0.1% chance

        // Rare tier
        if (roll < 0.011f) return VOID_ROOT;            // 1% chance (0.001 + 0.01)

        // Uncommon tier (1.2f multiplier roots)
        if (roll < 0.031f) return MYRIAD_BRANCHES_ROOT;   // 2% chance
        if (roll < 0.051f) return ABYSSAL_MOON_ROOT;    // 2% chance
        if (roll < 0.071f) return CELESTIAL_STEEL_ROOT; // 2% chance
        if (roll < 0.091f) return PHOENIX_ROOT;         // 2% chance
        if (roll < 0.111f) return DARK_MOUNTAIN_ROOT;   // 2% chance

        // Common tier (1.1f multiplier roots)
        if (roll < 0.151f) return MAHOGANY_ROOT;        // 4% chance
        if (roll < 0.191f) return WAVE_ROOT;            // 4% chance
        if (roll < 0.231f) return IRON_ROOT;            // 4% chance
        if (roll < 0.271f) return FLAME_ROOT;           // 4% chance
        if (roll < 0.311f) return STONE_ROOT;           // 4% chance

        // Basic tier - most common
        return MORTAL_ROOT;                             // 68.9% chance
    }
}
