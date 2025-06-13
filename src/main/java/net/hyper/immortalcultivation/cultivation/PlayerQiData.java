package net.hyper.immortalcultivation.cultivation;

import net.minecraft.nbt.CompoundTag;

public interface PlayerQiData {

    // === Core Data Methods ===

    /**
     * Deserializes Qi data from NBT
     * @param tag The NBT compound tag containing the data
     */
    void deserializeNBT(CompoundTag tag);

    /**
     * Serializes Qi data to NBT
     * @return CompoundTag containing the serialized data
     */
    CompoundTag serializeNBT();

    // === Qi Amount Methods ===

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
        return newAmount - current; // Return actual amount added
    }

    /**
     * Consumes Qi from the current amount
     * @param amount Amount to consume
     * @return true if successful, false if insufficient Qi
     */
    default boolean consumeQi(long amount) {
        if (getCurrentQi() >= amount) {
            setCurrentQi(getCurrentQi() - amount);
            return true;
        }
        return false;
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
     * @return true if player has at least this much Qi
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

    // === Regeneration Methods ===

    /**
     * Gets the base Qi regeneration rate per second
     * @return Regeneration rate
     */
    double getBaseQiRegenerationRate();

    /**
     * Gets the accelerated Qi regeneration rate per second
     * @return Accelerated regeneration rate
     */
    double getBaseAcceleratedQiRegenerationRate();

    /**
     * Gets the effective Qi regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveQiRegenerationRate() {
        return getBaseQiRegenerationRate(); // Override for modifiers
    }

    /**
     * Gets the effective Qi regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveAcceleratedQiRegenerationRate() {
        return getBaseAcceleratedQiRegenerationRate(); // Override for modifiers
    }

    /**
     * Checks if Qi regeneration is currently active
     * @return true if Qi should regenerate
     */
    default boolean canRegenerateQi() {
        return getCurrentQi() < getMaxQi();
    }

    // === Capacity Methods ===

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
        return getBaseMaxQi(); // Override for modifiers
    }

    // === State Methods ===

    /**
     * Checks if Qi is at maximum capacity
     * @return true if Qi is full
     */
    default boolean isQiFull() {
        return getCurrentQi() >= getMaxQi();
    }

    /**
     * Checks if Qi is completely empty
     * @return true if Qi is empty
     */
    default boolean isQiEmpty() {
        return getCurrentQi() <= 0;
    }

    /**
     * Gets remaining Qi capacity
     * @return How much more Qi can be stored
     */
    default long getRemainingCapacity() {
        return getMaxQi() - getCurrentQi();
    }

    // === Tick Method ===

    /**
     * Called every tick to update Qi state
     * Override this method for custom regeneration logic
     */
    default void tick() {
        // Only regenerate if not at max capacity
        if (canRegenerateQi()) {
            double regenRate = getEffectiveQiRegenerationRate();

            // Handle fractional regeneration properly
            if (this instanceof QiRegenerationState state) {
                state.addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (state.getRegenFraction() >= 1.0) {
                    long qiToAdd = (long) state.getRegenFraction();
                    state.setRegenFraction(state.getRegenFraction() - qiToAdd);
                    addQi(qiToAdd);
                }
            } else {
                // Simple regeneration without fractional tracking
                long regenAmount = Math.round(regenRate / 20.0);
                if (regenAmount > 0) {
                    addQi(regenAmount);
                }
            }
        }
    }

    // === Events/Callbacks ===

    /**
     * Called when Qi changes (for syncing, effects, etc.)
     * @param oldQi Previous Qi amount
     * @param newQi New Qi amount
     */
    default void onQiChanged(long oldQi, long newQi) {
        // Override for custom behavior
    }

    /**
     * Called when Qi is consumed
     * @param amount Amount consumed
     */
    default void onQiConsumed(long amount) {
        // Override for custom behavior
    }

    /**
     * Called when Qi is added/regenerated
     * @param amount Amount added
     */
    default void onQiAdded(long amount) {
        // Override for custom behavior
    }
}

/**
 * Helper interface for implementations that need fractional regeneration tracking
 */
interface QiRegenerationState {
    double getRegenFraction();
    void setRegenFraction(double fraction);

    default void addRegenFraction(double amount) {
        setRegenFraction(getRegenFraction() + amount);
    }
}
