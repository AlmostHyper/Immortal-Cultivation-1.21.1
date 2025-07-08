package net.hyper.immortalcultivation.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface IQiData {

    // === Qi === //

    /**
     * Gets the current Qi amount
     * @return Current Qi value
     */
    long getCurrentQi();

    /**
     * Sets the current Qi amount (clamped between 0 and max)
     * @param qi The new Qi amount
     */
    void setCurrentQi(long qi);

    /**
     * Adds Qi to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addQi(long amount) {
        long current = getCurrentQi();
        long max = getMaxQi();
        long newAmount = Math.min(current + amount, max);
        setCurrentQi(newAmount);
        return newAmount - current;
    }

    /**
     * Consumes Qi from the current amount
     *
     * @param amount Amount to consume
     */
    default void consumeQi(long amount) {
        if (getCurrentQi() >= amount) {
            setCurrentQi(getCurrentQi() - amount);
        }
    }

    /**
     * Tries to consume Qi, consuming as much as possible
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeQi(long amount) {
        long current = getCurrentQi();
        long toConsume = Math.min(current, amount);
        setCurrentQi(current - toConsume);
        return toConsume;
    }

    /**
     * Checks if the player has enough Qi
     * @param amount Amount to check
     * @return True if player has at least this much Qi
     */
    default boolean hasQi(long amount) {
        return getCurrentQi() >= amount;
    }

    /**
     * Gets the Qi as a percentage of maximum (0.0 to 1.0)
     * @return Qi percentage
     */
    default double getQiPercentage() {
        long max = getMaxQi();
        return max > 0 ? (double) getCurrentQi() / max : 0.0;
    }

    // === Regeneration === //

    /**
     * Gets the base Qi regeneration rate per second
     * @return Regeneration rate
     */
    double getBaseQiRegenerationRate();

    /**
     * Gets the effective Qi regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveQiRegenerationRate() {
        return getBaseQiRegenerationRate(); // Override for modifiers
    }

    /**
     * Checks if Qi regeneration is currently active
     * @return True if Qi should regenerate
     */
    default boolean canRegenerateQi() {
        return getCurrentQi() < getMaxQi();
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
    default void addRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }

    // === Capacity === //

    /**
     * Gets the base maximum Qi (without modifiers)
     * @return Base maximum Qi
     */
    long getBaseMaxQi();

    /**
     * Gets the effective maximum Qi (including modifiers)
     * Default implementation returns base max Qi - override for modifiers
     * @return Effective maximum Qi
     */
    default long getMaxQi() {
        return getBaseMaxQi();
    }

    // === State  === //

    /**
     * Checks if Qi is at maximum capacity
     * @return True if Qi is full
     */
    default boolean isQiFull() {
        return getCurrentQi() >= getMaxQi();
    }

    /**
     * Checks if Qi is completely empty
     * @return True if Qi is empty
     */
    default boolean isQiEmpty() {
        return getCurrentQi() <= 0;
    }

    /**
     * Gets remaining Qi capacity
     * @return How much more Qi can be stored
     */
    default long getRemainingQiCapacity() {
        return getMaxQi() - getCurrentQi();
    }

    // === Tick === //

    /**
     * Called every tick to update Qi state
     */
    default void tick() {
        // Only cultivate if not at max capacity
        if (canRegenerateQi()) {
            double regenRate = getEffectiveQiRegenerationRate();

            if (regenRate > 0) {
                // Use the built-in regen fraction methods
                addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) getRegenFraction();
                    setRegenFraction(getRegenFraction() - cultivationToAdd);
                    addQi(cultivationToAdd);
                }
            }
        }
    }

    // === Events === //

    /**
     * Called when Qi changes (for syncing, effects, etc.)
     * @param oldQi Previous Qi amount
     * @param newQi New Qi amount
     */
    default void onQiChanged(long oldQi, long newQi) {
    }

    /**
     * Called when Qi is consumed
     * @param amount Amount consumed
     */
    default void onQiConsumed(long amount) {
    }

    /**
     * Called when Qi is added/regenerated
     * @param amount Amount added
     */
    default void onQiAdded(long amount) {
    }
}
