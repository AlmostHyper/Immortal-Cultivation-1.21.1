package net.hyper.immortalcultivation.cultivation;

import net.minecraft.nbt.CompoundTag;

public interface IPlayerCultivationData {

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
     * @param cultivation The new Cultivation amount
     */
    void setCurrentCultivation(long cultivation);

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
     * Gets the base Cultivation regeneration rate per second
     * @return Regeneration rate
     */
    double getBaseCultivationRegenerationRate();

    /**
     * Gets the accelerated Cultivation regeneration rate per second
     * @return Accelerated regeneration rate
     */
    double getBaseAcceleratedCultivationRegenerationRate();

    /**
     * Gets the effective Cultivation regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveCultivationRegenerationRate() {
        return getBaseCultivationRegenerationRate(); // Override for modifiers
    }

    /**
     * Gets the effective Cultivation regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveAcceleratedCultivationRegenerationRate() {
        return getBaseAcceleratedCultivationRegenerationRate(); // Override for modifiers
    }

    /**
     * Checks if Cultivation regeneration is currently active
     * @return true if Cultivation should regenerate
     */
    default boolean canRegenerateCultivation() {
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
        // Only regenerate if not at max capacity
        if (canRegenerateCultivation()) {
            double regenRate = getEffectiveCultivationRegenerationRate();

            // Handle fractional regeneration properly
            if (this instanceof CultivationRegenerationState state) {
                state.addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (state.getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) state.getRegenFraction();
                    state.setRegenFraction(state.getRegenFraction() - cultivationToAdd);
                    addCultivation(cultivationToAdd);
                }
            } else {
                // Simple regeneration without fractional tracking
                long regenAmount = Math.round(regenRate / 20.0);
                if (regenAmount > 0) {
                    addCultivation(regenAmount);
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
}

/**
 * Helper interface for implementations that need fractional regeneration tracking
 */
interface CultivationRegenerationState {
    double getRegenFraction();
    void setRegenFraction(double fraction);

    default void addRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }
}
