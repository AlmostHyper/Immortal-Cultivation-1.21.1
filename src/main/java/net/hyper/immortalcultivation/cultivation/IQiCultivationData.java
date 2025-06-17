package net.hyper.immortalcultivation.cultivation;

import net.minecraft.nbt.CompoundTag;

public interface IQiCultivationData {

    // === Core Data Methods ===

    /**
     * Deserializes Cultivation data from NBT
     * @param tag The NBT compound tag containing the data
     */
    void deserializeNBT(CompoundTag tag);

    /**
     * Serializes Cultivation data to NBT
     * @return CompoundTag containing the serialized data
     */
    CompoundTag serializeNBT();

    // === Cultivation Amount Methods ===

    /**
     * Gets the current Cultivation amount
     * @return Current Cultivation value
     */
    long getCurrentCultivation();

    /**
     * Sets the current Cultivation amount (clamped between 0 and max)
     *
     * @param cultivation The new Cultivation amount
     * @return
     */
    long setCurrentCultivation(long cultivation);

    /**
     * Adds Cultivation to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addCultivation(long amount) {
        long current = getCurrentCultivation();
        long max = getMaxCultivation();
        long newAmount = Math.min(current + amount, max);
        setCurrentCultivation(newAmount);
        return newAmount - current; // Return actual amount added
    }

    /**
     * Applies spiritual root cultivation bonuses to cultivation gains
     * @return Spiritual root bonus
     */
    double addCultivationWithSpiritualRoots(long baseAmount);

    /**
     * Consumes Cultivation from the current amount
     * @param amount Amount to consume
     * @return true if successful, false if insufficient Cultivation
     */
    default boolean consumeCultivation(long amount) {
        if (getCurrentCultivation() >= amount) {
            setCurrentCultivation(getCurrentCultivation() - amount);
            return true;
        }
        return false;
    }

    /**
     * Tries to consume Cultivation, consuming as much as possible
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeCultivation(long amount) {
        long current = getCurrentCultivation();
        long toConsume = Math.min(current, amount);
        setCurrentCultivation(current - toConsume);
        return toConsume;
    }

    /**
     * Checks if the player has enough Cultivation
     * @param amount Amount to check
     * @return true if player has at least this much Cultivation
     */
    default boolean hasCultivation(long amount) {
        return getCurrentCultivation() >= amount;
    }

    /**
     * Gets the Cultivation as a percentage of maximum (0.0 to 1.0)
     * @return Cultivation percentage
     */
    default double getCultivationPercentage() {
        long max = getMaxCultivation();
        return max > 0 ? (double) getCurrentCultivation() / max : 0.0;
    }

    // === Regeneration Methods ===

    /**
     * Gets the base Cultivation rate per second
     * @return Cultivation rate
     */
    double getBaseCultivationRate();

    /**
     * Gets the base Meditation Cultivation rate per second
     * @return Meditation cultivation rate
     */
    double getBaseMeditationCultivationRate();

    /**
     * Gets the effective Cultivation rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveCultivationRate() {
        return getBaseCultivationRate(); // Override for modifiers
    }

    /**
     * Gets the effective Meditation Cultivation rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveMeditationCultivationRate() {
        return getBaseMeditationCultivationRate(); // Override for modifiers
    }

    /**
     * Checks if Cultivation is allowed
     * @return true if Cultivation should increase
     */
    default boolean canCultivate() {
        return getCurrentCultivation() < getMaxCultivation();
    }

    // === Capacity Methods ===

    /**
     * Gets the base maximum Cultivation (without modifiers)
     * @return Base maximum Cultivation
     */
    long getBaseMaxCultivation();

    /**
     * Gets the effective maximum Cultivation (including modifiers)
     * Default implementation returns base max Cultivation - override for modifiers
     * @return Effective maximum Cultivation
     */
    default long getMaxCultivation() {
        return getBaseMaxCultivation(); // Override for modifiers
    }

    // === State Methods ===

    /**
     * Checks if Cultivation is at maximum capacity
     * @return true if Cultivation is full
     */
    default boolean isCultivationFull() {
        return getCurrentCultivation() >= getMaxCultivation();
    }

    /**
     * Checks if Cultivation is completely empty
     * @return true if Cultivation is empty
     */
    default boolean isCultivationEmpty() {
        return getCurrentCultivation() <= 0;
    }

    /**
     * Gets remaining Cultivation capacity
     * @return How much more Cultivation can be stored
     */
    default long getRemainingCultivationCapacity() {
        return getMaxCultivation() - getCurrentCultivation();
    }

    // === Tick Method ===

    /**
     * Called every tick to update Cultivation state
     * Override this method for custom regeneration logic
     */
    default void tick() {
        // Only cultivate if not at max capacity
        if (canCultivate()) {
            double regenRate = getEffectiveCultivationRate();

            if (regenRate > 0) {
                // Use the built-in regen fraction methods
                addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) getRegenFraction();
                    setRegenFraction(getRegenFraction() - cultivationToAdd);
                    addCultivation(cultivationToAdd);
                }
            }
        }
    }

    // === Events/Callbacks ===

    /**
     * Called when Cultivation changes (for syncing, effects, etc.)
     * @param oldCultivation Previous Cultivation amount
     * @param newCultivation New Cultivation amount
     */
    default void onCultivationChanged(long oldCultivation, long newCultivation) {
        // Override for custom behavior
    }

    /**
     * Called when Cultivation is consumed
     * @param amount Amount consumed
     */
    default void onCultivationConsumed(long amount) {
        // Override for custom behavior
    }

    /**
     * Called when Cultivation is added/regenerated
     * @param amount Amount added
     */
    default void onCultivationAdded(long amount) {
        // Override for custom behavior
    }

    double getRegenFraction();
    void setRegenFraction(double fraction);

    default void addRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }
}
