package net.hyper.immortalcultivation.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface IBodyCultivationData {

    // === Body Cultivation === //

    /**
     * Gets the current Body Cultivation amount
     * @return Current Body Cultivation value
     */
    long getCurrentBodyCultivation();

    /**
     * Sets the current Body Cultivation amount (clamped between 0 and max)
     * @param bodyCultivation The new Body Cultivation amount
     */
    void setCurrentBodyCultivation(long bodyCultivation);

    /**
     * Adds Body Cultivation to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addBodyCultivation(long amount) {
        long current = getCurrentBodyCultivation();
        long max = getMaxBodyCultivation();
        long newAmount = Math.min(current + amount, max);
        setCurrentBodyCultivation(newAmount);
        return newAmount - current;
    }

    /**
     * Consumes Body Cultivation from the current amount
     * @param amount Amount to consume
     * @return True if successful, false if insufficient Body Cultivation
     */
    default boolean consumeBodyCultivation(long amount) {
        if (getCurrentBodyCultivation() >= amount) {
            setCurrentBodyCultivation(getCurrentBodyCultivation() - amount);
            return true;
        }
        return false;
    }

    /**
     * Tries to consume Body Cultivation, consuming as much as possible
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeBodyCultivation(long amount) {
        long current = getCurrentBodyCultivation();
        long toConsume = Math.min(current, amount);
        setCurrentBodyCultivation(current - toConsume);
        return toConsume;
    }

    /**
     * Checks if the player has enough Body Cultivation
     * @param amount Amount to check
     * @return True if player has at least this much Body
     */
    default boolean hasBodyCultivation(long amount) {
        return getCurrentBodyCultivation() >= amount;
    }

    /**
     * Gets the Body Cultivation as a percentage of maximum (0.0 to 1.0)
     * @return Body Cultivation percentage
     */
    default double getBodyCultivationPercentage() {
        long max = getMaxBodyCultivation();
        return max > 0 ? (double) getCurrentBodyCultivation() / max : 0.0;
    }

    // === Regeneration === //

    /**
     * Gets the base Body Cultivation regeneration rate per second
     * @return Body Cultivation rate
     */
    double getBaseBodyCultivationRate();

    /**
     * Gets the base circulating Body Cultivation regeneration rate per second
     * @return Circulating Body Cultivation rate
     */
    double getBaseCirculatingBodyCultivationRate();

    /**
     * Gets the effective Body Cultivation rate (including modifiers)
     * @return Effective Body Cultivation rate
     */
    default double getEffectiveBodyCultivationRate() {
        return getBaseBodyCultivationRate();
    }

    /**
     * Gets the effective circulating Body Cultivation regeneration rate (including modifiers)
     * @return Effective circulating Body Cultivation rate
     */
    default double getEffectiveCirculatingBodyCultivationRate() {
        return getBaseCirculatingBodyCultivationRate();
    }

    /**
     * Checks if Body Cultivation is currently active
     * @return True if Body Cultivation should progress
     */
    default boolean canCultivateBody() {
        return getCurrentBodyCultivation() < getMaxBodyCultivation();
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
    default void addBodyRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }

    // === Capacity === //

    /**
     * Gets the base maximum Body Cultivation (without modifiers)
     * @return Base maximum Body Cultivation
     */
    long getBaseBodyCultivation();

    /**
     * Gets the effective maximum Body Cultivation (including modifiers)
     * Default implementation returns base max Body Cultivation - override for modifiers
     * @return Effective maximum Body Cultivation
     */
    default long getMaxBodyCultivation() {
        return getBaseBodyCultivation();
    }

    // === State  === //

    /**
     * Checks if Body Cultivation is at maximum capacity
     * @return True if Body Cultivation is full
     */
    default boolean isBodyCultivationFull() {
        return getBodyCultivationPercentage() >= getMaxBodyCultivation();
    }

    /**
     * Checks if Body Cultivation is completely empty
     * @return True if Body Cultivation is empty
     */
    default boolean isBodyCultivationEmpty() {
        return getCurrentBodyCultivation() <= 0;
    }

    /**
     * Gets remaining Body Cultivation capacity
     * @return How much more Body Cultivation can be stored
     */
    default long getRemainingBodyCultivationCapacity() {
        return getMaxBodyCultivation() - getCurrentBodyCultivation();
    }

    // === Tick === //

    /**
     * Called every tick to update Body state
     */
    default void tick() {
        // Only cultivate if not at max capacity
        if (canCultivateBody()) {
            double regenRate = getEffectiveBodyCultivationRate();

            if (regenRate > 0) {
                // Use the built-in regen fraction methods
                addBodyRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) getRegenFraction();
                    setRegenFraction(getRegenFraction() - cultivationToAdd);
                    addBodyCultivation(cultivationToAdd);
                }
            }
        }
    }

    // === Events === //

    /**
     * Called when Body Cultivation changes (for syncing, effects, etc.)
     * @param oldBodyCultivation Previous Body Cultivation amount
     * @param newBodyCultivation New Body Cultivation amount
     */
    default void onBodyCultivationChanged(long oldBodyCultivation, long newBodyCultivation) {
    }

    /**
     * Called when Body Cultivation is consumed
     * @param amount Amount consumed
     */
    default void onBodyCultivationConsumed(long amount) {
    }

    /**
     * Called when Body Cultivation is added/regenerated
     * @param amount Amount added
     */
    default void onBodyCultivationAdded(long amount) {
    }
}
