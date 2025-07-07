package net.hyper.immortalcultivation.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface IQiCultivationData {

    // === Qi Cultivation === //

    /**
     * Gets the current Qi Cultivation amount
     * @return Current Qi Cultivation value
     */
    long getCurrentQiCultivation();

    /**
     * Sets the current Qi Cultivation amount (clamped between 0 and max)
     * @param qiCultivation The new Qi Cultivation amount
     */
    void setCurrentQiCultivation(long qiCultivation);

    /**
     * Adds Qi Cultivation to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addQiCultivation(long amount) {
        long current = getCurrentQiCultivation();
        long max = getMaxQiCultivation();
        long newAmount = Math.min(current + amount, max);
        setCurrentQiCultivation(newAmount);
        return newAmount - current;
    }

    /**
     * Consumes Qi Cultivation from the current amount
     * @param amount Amount to consume
     * @return True if successful, false if insufficient Qi Cultivation
     */
    default boolean consumeQiCultivation(long amount) {
        if (getCurrentQiCultivation() >= amount) {
            setCurrentQiCultivation(getCurrentQiCultivation() - amount);
            return true;
        }
        return false;
    }

    /**
     * Tries to consume Qi Cultivation, consuming as much as possible
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeQiCultivation(long amount) {
        long current = getCurrentQiCultivation();
        long toConsume = Math.min(current, amount);
        setCurrentQiCultivation(current - toConsume);
        return toConsume;
    }

    /**
     * Checks if the player has enough Qi Cultivation
     * @param amount Amount to check
     * @return True if player has at least this much Body
     */
    default boolean hasQiCultivation(long amount) {
        return getCurrentQiCultivation() >= amount;
    }

    /**
     * Gets the Qi Cultivation as a percentage of maximum (0.0 to 1.0)
     * @return Qi Cultivation percentage
     */
    default double getQiCultivationPercentage() {
        long max = getMaxQiCultivation();
        return max > 0 ? (double) getCurrentQiCultivation() / max : 0.0;
    }

    // === Regeneration === //

    /**
     * Gets the base Qi Cultivation regeneration rate per second
     * @return Qi Cultivation rate
     */
    double getBaseQiCultivationRate();

    /**
     * Gets the base circulating Qi Cultivation regeneration rate per second
     * @return Circulating Qi Cultivation rate
     */
    double getBaseCirculatingQiCultivationRate();

    /**
     * Gets the effective Qi Cultivation rate (including modifiers)
     * @return Effective Qi Cultivation rate
     */
    default double getEffectiveQiCultivationRate() {
        return getBaseQiCultivationRate();
    }

    /**
     * Gets the effective circulating Qi Cultivation regeneration rate (including modifiers)
     * @return Effective circulating Qi Cultivation rate
     */
    default double getEffectiveCirculatingQiCultivationRate() {
        return getBaseCirculatingQiCultivationRate();
    }

    /**
     * Checks if Qi Cultivation is currently active
     * @return True if Qi Cultivation should progress
     */
    default boolean canCultivateQi() {
        return getCurrentQiCultivation() < getMaxQiCultivation();
    }

    /**
     * Gets the fraction of current regeneration
     * @return Fractional regeneration rate
     */
    double getRegenFraction();

    /**
     * Sets the fraction of current regeneration
     * @param fraction The new fractional amount
     */
    void setRegenFraction(double fraction);

    /**
     * Adds to fractional rate
     */
    default void addQiRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }

    // === Capacity === //

    /**
     * Gets the base maximum Qi Cultivation (without modifiers)
     * @return Base maximum Qi Cultivation
     */
    long getBaseQiCultivation();

    /**
     * Gets the effective maximum Qi Cultivation (including modifiers)
     * Default implementation returns base max Qi Cultivation - override for modifiers
     * @return Effective maximum Qi Cultivation
     */
    default long getMaxQiCultivation() {
        return getBaseQiCultivation();
    }

    // === State  === //

    /**
     * Checks if Qi Cultivation is at maximum capacity
     * @return True if Qi Cultivation is full
     */
    default boolean isQiCultivationFull() {
        return getQiCultivationPercentage() >= getMaxQiCultivation();
    }

    /**
     * Checks if Qi Cultivation is completely empty
     * @return True if Qi Cultivation is empty
     */
    default boolean isQiCultivationEmpty() {
        return getCurrentQiCultivation() <= 0;
    }

    /**
     * Gets remaining Qi Cultivation capacity
     * @return How much more Qi Cultivation can be stored
     */
    default long getRemainingQiCultivationCapacity() {
        return getMaxQiCultivation() - getCurrentQiCultivation();
    }

    // === Tick === //

    /**
     * Called every tick to update Qi Cultivation state
     */
    default void tick() {
        // Only cultivate if not at max capacity
        if (canCultivateQi()) {
            double regenRate = getEffectiveQiCultivationRate();

            if (regenRate > 0) {
                // Use the built-in regen fraction methods
                addQiRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) getRegenFraction();
                    setRegenFraction(getRegenFraction() - cultivationToAdd);
                    addQiCultivation(cultivationToAdd);
                }
            }
        }
    }

    // === Events === //

    /**
     * Called when Qi Cultivation changes (for syncing, effects, etc.)
     * @param oldQiCultivation Previous Qi Cultivation amount
     * @param newQiCultivation New Qi Cultivation amount
     */
    default void onQiCultivationChanged(long oldQiCultivation, long newQiCultivation) {
    }

    /**
     * Called when Qi Cultivation is consumed
     * @param amount Amount consumed
     */
    default void onQiCultivationConsumed(long amount) {
    }

    /**
     * Called when Qi Cultivation is added/regenerated
     * @param amount Amount added
     */
    default void onQiCultivationAdded(long amount) {
    }
}
