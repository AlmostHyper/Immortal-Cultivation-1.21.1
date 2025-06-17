package net.hyper.immortalcultivation.cultivation;

import net.minecraft.util.RandomSource;

public enum PhysiqueType {
    MORTAL_PHYSIQUE(1.0f, "Mortal Physique", 0x8B4513),
    STONE_PHYSIQUE(1.1f, "Stone Physique", 0x32CD32),
    FLAME_PHYSIQUE(1.1f, "Flame Physique", 0x4169E1),
    IRON_PHYSIQUE(1.1f, "Iron Physique", 0xFFD700),
    WAVE_PHYSIQUE(1.1f, "Wave Physique", 0xFFD700),
    MAHOGANY_PHYSIQUE(1.1f, "Mahogany Physique", 0xFFD700),
    DARK_MOUNTAIN_PHYSIQUE(1.2f, "Dark Mountain Physique", 0x32CD32),
    PHOENIX_PHYSIQUE(1.2f, "Phoenix Physique", 0x4169E1),
    CELESTIAL_STEEL_PHYSIQUE(1.2f, "Celestial Steel Physique", 0xFFD700),
    ABYSSAL_MOON_PHYSIQUE(1.2f, "Abyssal Moon Physique", 0xFFD700),
    MYRIAD_BRANCH_PHYSIQUE(1.2f, "Myriad Branch Physique", 0xFFD700),
    VOID_PHYSIQUE(1.3f, "Void Physique", 0xFFD700),
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

        // Legendary tier
        if (roll < 0.001f) return HEAVENLY_PHYSIQUE;          // 0.1% chance

        // Rare tier
        if (roll < 0.011f) return VOID_PHYSIQUE;            // 1% chance (0.001 + 0.01)

        // Uncommon tier (1.2f multiplier physiques)
        if (roll < 0.031f) return MYRIAD_BRANCH_PHYSIQUE;   // 2% chance
        if (roll < 0.051f) return ABYSSAL_MOON_PHYSIQUE;    // 2% chance
        if (roll < 0.071f) return CELESTIAL_STEEL_PHYSIQUE; // 2% chance
        if (roll < 0.091f) return PHOENIX_PHYSIQUE;         // 2% chance
        if (roll < 0.111f) return DARK_MOUNTAIN_PHYSIQUE;   // 2% chance

        // Common tier (1.1f multiplier physiques)
        if (roll < 0.151f) return MAHOGANY_PHYSIQUE;        // 4% chance
        if (roll < 0.191f) return WAVE_PHYSIQUE;            // 4% chance
        if (roll < 0.231f) return IRON_PHYSIQUE;            // 4% chance
        if (roll < 0.271f) return FLAME_PHYSIQUE;           // 4% chance
        if (roll < 0.311f) return STONE_PHYSIQUE;           // 4% chance

        // Basic tier - most common
        return MORTAL_PHYSIQUE;                             // 68.9% chance
    }
}
