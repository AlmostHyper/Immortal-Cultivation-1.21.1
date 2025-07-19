package net.hyper.immortalcultivation.core.attachments;

/**
 * Unified stats interface supporting multiple resource types (Health, Qi, Body)
 * following the same pattern as ICultivationData for consistent and maintainable code.
 */
public interface IStatsData {

    /**
     * Enum representing the different stat types that can be managed.
     */
    enum StatType {
        HEALTH("Health"),
        QI("Qi"),
        BODY("Body");

        private final String displayName;

        StatType(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Gets the display name for this stat type.
         * @return The human-readable name
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    // === CORE STAT MANAGEMENT === //

    /**
     * Gets the current value for the specified stat type.
     * @param type The stat type to query
     * @return Current stat value
     */
    long getCurrentStat(StatType type);

    /**
     * Sets the current value for the specified stat type (clamped between 0 and max).
     * @param type The stat type to modify
     * @param value The new stat value
     */
    void setCurrentStat(StatType type, long value);

    /**
     * Gets the maximum value for the specified stat type.
     * @param type The stat type to query
     * @return Maximum stat value
     */
    long getMaxStat(StatType type);

    /**
     * Sets the maximum value for the specified stat type.
     * @param type The stat type to modify
     * @param value The new maximum value
     */
    void setMaxStat(StatType type, long value);

    /**
     * Adds to the current stat amount, respecting the maximum.
     * @param type The stat type to modify
     * @param amount Amount to add
     * @return Actual amount added (may be less than requested due to maximum)
     */
    default long addStat(StatType type, long amount) {
        long current = getCurrentStat(type);
        long max = getMaxStat(type);
        long newAmount = Math.min(current + amount, max);
        setCurrentStat(type, newAmount);
        return newAmount - current;
    }

    /**
     * Consumes from the current stat amount.
     * @param type The stat type to modify
     * @param amount Amount to consume
     * @return Actual amount consumed (may be less than requested if insufficient)
     */
    default long consumeStat(StatType type, long amount) {
        long current = getCurrentStat(type);
        long toConsume = Math.min(current, amount);
        setCurrentStat(type, current - toConsume);
        return toConsume;
    }

    /**
     * Tries to consume from the stat, consuming as much as possible.
     * @param type The stat type to modify
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeStat(StatType type, long amount) {
        return consumeStat(type, amount);
    }

    /**
     * Checks if the specified amount of stat is available.
     * @param type The stat type to check
     * @param amount Amount to check for
     * @return True if the stat value is at least the specified amount
     */
    default boolean hasStat(StatType type, long amount) {
        return getCurrentStat(type) >= amount;
    }

    /**
     * Gets the stat as a percentage of maximum (0.0 to 1.0).
     * @param type The stat type to calculate percentage for
     * @return Stat percentage (0.0 to 1.0)
     */
    default double getStatPercentage(StatType type) {
        long max = getMaxStat(type);
        return max > 0 ? (double) getCurrentStat(type) / max : 0.0;
    }

    // === REGENERATION SYSTEM === //

    /**
     * Gets the base regeneration rate per second for the specified stat type.
     * @param type The stat type to query
     * @return Base regeneration rate per second
     */
    double getStatRegenRate(StatType type);

    /**
     * Gets the effective regeneration rate including all modifiers.
     * @param type The stat type to query
     * @return Effective regeneration rate per second
     */
    default double getEffectiveStatRegenerationRate(StatType type) {
        return getStatRegenRate(type);
    }

    /**
     * Checks if the specified stat type can currently regenerate.
     * @param type The stat type to check
     * @return True if regeneration should occur
     */
    default boolean canRegenerateStat(StatType type) {
        return getCurrentStat(type) < getMaxStat(type);
    }

    /**
     * Gets the current regeneration fraction for the specified stat type.
     * Used for sub-integer regeneration accumulation.
     * @param type The stat type to query
     * @return Current regeneration fraction
     */
    double getRegenFraction(StatType type);

    /**
     * Sets the regeneration fraction for the specified stat type.
     * @param type The stat type to modify
     * @param fraction The new fractional regeneration amount
     */
    void setRegenFraction(StatType type, double fraction);

    /**
     * Adds to the regeneration fraction for the specified stat type.
     * @param type The stat type to modify
     * @param amount Amount to add to the fraction
     */
    default void addRegenFraction(StatType type, double amount) {
        setRegenFraction(type, getRegenFraction(type) + amount);
    }

    // === BONUSES AND MODIFIERS === //

    /**
     * Gets the permanent bonus that increases the maximum for the specified stat type.
     * @param type The stat type to query
     * @return Permanent bonus amount
     */
    long getPermanentStatBonus(StatType type);

    /**
     * Sets the permanent bonus for the specified stat type.
     * @param type The stat type to modify
     * @param bonus The new permanent bonus amount
     */
    void setPermanentStatBonus(StatType type, long bonus);

    /**
     * Gets the permanent regeneration bonus multiplier for the specified stat type.
     * @param type The stat type to query
     * @return Permanent regeneration bonus multiplier
     */
    double getPermanentStatRegenBonus(StatType type);

    /**
     * Sets the permanent regeneration bonus for the specified stat type.
     * @param type The stat type to modify
     * @param bonus The new regeneration bonus multiplier
     */
    void setPermanentStatRegenBonus(StatType type, double bonus);

    // === CALLBACK METHODS === //

    /**
     * Called when a stat value changes. Override for custom behavior.
     * @param type The stat type that changed
     * @param oldValue Previous stat value
     * @param newValue New stat value
     */
    default void onStatChanged(StatType type, long oldValue, long newValue) {}

    /**
     * Called when a stat is consumed. Override for custom behavior.
     * @param type The stat type that was consumed
     * @param amount Amount that was consumed
     */
    default void onStatConsumed(StatType type, long amount) {}

    /**
     * Called when a stat is added/regenerated. Override for custom behavior.
     * @param type The stat type that was added to
     * @param amount Amount that was added
     */
    default void onStatAdded(StatType type, long amount) {}
}