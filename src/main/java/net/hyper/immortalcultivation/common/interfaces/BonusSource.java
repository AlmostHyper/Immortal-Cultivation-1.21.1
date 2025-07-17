package net.hyper.immortalcultivation.common.interfaces;

import com.mojang.serialization.Codec;
import net.hyper.immortalcultivation.ImmortalCultivation;

/**
 * Enumeration of different sources that can provide bonuses to player stats.
 * This allows for precise bonus management and prevents bonus source conflicts.
 *
 * Each source can be cleared independently without affecting other sources,
 * enabling proper realm reapplication while preserving pill and other bonuses.
 */
public enum BonusSource {
    /**
     * Bonuses from cultivation realm advancement.
     * These bonuses are recalculated and reapplied on login/respawn.
     */
    REALM("Realm", true),

    /**
     * Bonuses from consuming cultivation pills.
     * These bonuses are permanent and based on consumption history.
     */
    PILL("Pill", false),

    /**
     * Bonuses from equipment and gear.
     * These bonuses are temporary and tied to equipped items.
     */
    EQUIPMENT("Equipment", true),

    /**
     * Bonuses from techniques and abilities.
     * These bonuses may be temporary or permanent based on technique type.
     */
    TECHNIQUE("Technique", false),

    /**
     * Bonuses from cultivation arts and manuals.
     * These bonuses are usually permanent once learned.
     */
    ART("Art", false),

    /**
     * Bonuses from bloodline or racial heritage.
     * These bonuses are permanent and intrinsic to the character.
     */
    BLOODLINE("Bloodline", false),

    /**
     * Temporary bonuses from effects, buffs, or environmental factors.
     * These bonuses are explicitly temporary and managed separately.
     */
    TEMPORARY("Temporary", true),

    /**
     * Bonuses from cultivation milestones (every 1000 cultivation points).
     * These bonuses are permanent and calculated based on total cultivation progress.
     */
    MILESTONES("Milestones", false),

    /**
     * Bonuses from QI cultivation milestones (every 1000 cultivation points).
     * These bonuses are permanent and calculated based on QI cultivation progress.
     */
    QI_MILESTONES("Qi Milestones", false),

    /**
     * Bonuses from BODY cultivation milestones (every 1000 cultivation points).
     * These bonuses are permanent and calculated based on BODY cultivation progress.
     */
    BODY_MILESTONES("Body Milestones", false);

    private final String displayName;
    private final boolean recalculatedOnLogin;

    /**
     * Creates a new bonus source with specified properties.
     *
     * @param displayName Human-readable name for this bonus source
     * @param recalculatedOnLogin Whether this bonus should be recalculated on player login
     */
    BonusSource(String displayName, boolean recalculatedOnLogin) {
        this.displayName = displayName;
        this.recalculatedOnLogin = recalculatedOnLogin;
    }

    /**
     * Gets the display name for this bonus source.
     * @return Human-readable name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this bonus source should be recalculated on player login.
     * Sources marked as recalculated are typically cleared and reapplied during login.
     *
     * @return true if bonuses from this source should be recalculated on login
     */
    public boolean isRecalculatedOnLogin() {
        return recalculatedOnLogin;
    }

    /**
     * Gets all bonus sources that should be recalculated on login.
     * @return Array of bonus sources that need recalculation
     */
    public static BonusSource[] getRecalculatedSources() {
        return java.util.Arrays.stream(values())
            .filter(BonusSource::isRecalculatedOnLogin)
            .toArray(BonusSource[]::new);
    }

    /**
     * Gets all bonus sources that are preserved between sessions.
     * @return Array of bonus sources that persist across logins
     */
    public static BonusSource[] getPersistentSources() {
        return java.util.Arrays.stream(values())
            .filter(source -> !source.isRecalculatedOnLogin())
            .toArray(BonusSource[]::new);
    }

    /**
     * Codec for BonusSource enum serialization.
     * Handles unknown sources gracefully by defaulting to REALM.
     */
    public static final Codec<BonusSource> CODEC = Codec.STRING.xmap(
        name -> {
            try {
                return BonusSource.valueOf(name);
            } catch (IllegalArgumentException e) {
                ImmortalCultivation.LOGGER.warn("Unknown bonus source: {}, defaulting to REALM", name);
                return BonusSource.REALM; // Safe default
            }
        },
        BonusSource::name
    ).stable(); // Mark as stable for better performance
}