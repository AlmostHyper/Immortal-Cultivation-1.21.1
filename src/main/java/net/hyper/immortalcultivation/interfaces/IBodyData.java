package net.hyper.immortalcultivation.interfaces;

import net.minecraft.nbt.CompoundTag;

public interface IBodyData {

    // === Body === //

    /**
     * Gets the current Body amount
     * @return Current Body value
     */
    long getCurrentBody();

    /**
     * Sets the current Body amount (clamped between 0 and max)
     * @param body The new Body amount
     */
    void setCurrentBody(long body);

    /**
     * Adds Body to the current amount (respects maximum)
     * @param amount Amount to add
     * @return Actual amount added
     */
    default long addBody(long amount) {
        long current = getCurrentBody();
        long max = getMaxBody();
        long newAmount = Math.min(current + amount, max);
        setCurrentBody(newAmount);
        return newAmount - current;
    }

    /**
     * Consumes Body from the current amount
     *
     * @param amount Amount to consume
     */
    default void consumeBody(long amount) {
        if (getCurrentBody() >= amount) {
            setCurrentBody(getCurrentBody() - amount);
        }
    }

    /**
     * Tries to consume Body, consuming as much as possible
     * @param amount Desired amount to consume
     * @return Actual amount consumed
     */
    default long tryConsumeBody(long amount) {
        long current = getCurrentBody();
        long toConsume = Math.min(current, amount);
        setCurrentBody(current - toConsume);
        return toConsume;
    }

    /**
     * Checks if the player has enough Body
     * @param amount Amount to check
     * @return True if player has at least this much Body
     */
    default boolean hasBody(long amount) {
        return getCurrentBody() >= amount;
    }

    /**
     * Gets the Body as a percentage of maximum (0.0 to 1.0)
     * @return Body percentage
     */
    default double getBodyPercentage() {
        long max = getMaxBody();
        return max > 0 ? (double) getCurrentBody() / max : 0.0;
    }

    // === Regeneration === //

    /**
     * Gets the base Body regeneration rate per second
     * @return Regeneration rate
     */
    double getBaseBodyRegenerationRate();

    /**
     * Gets the effective Body regeneration rate (including modifiers)
     * @return Effective regeneration rate
     */
    default double getEffectiveBodyRegenerationRate() {
        return getBaseBodyRegenerationRate(); // Override for modifiers
    }

    /**
     * Checks if Body regeneration is currently active
     * @return True if Body should regenerate
     */
    default boolean canRegenerateBody() {
        return getCurrentBody() < getMaxBody();
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
     * Gets the base maximum Body (without modifiers)
     * @return Base maximum Body
     */
    long getBaseMaxBody();

    /**
     * Gets the effective maximum Body (including modifiers)
     * Default implementation returns base max Body - override for modifiers
     * @return Effective maximum Body
     */
    default long getMaxBody() {
        return getBaseMaxBody(); // Override for modifiers
    }

    // === State  === //

    /**
     * Checks if Body is at maximum capacity
     * @return True if Body is full
     */
    default boolean isBodyFull() {
        return getCurrentBody() >= getMaxBody();
    }

    /**
     * Checks if Body is completely empty
     * @return True if Body is empty
     */
    default boolean isBodyEmpty() {
        return getCurrentBody() <= 0;
    }

    /**
     * Gets remaining Body capacity
     * @return How much more Body can be stored
     */
    default long getRemainingBodyCapacity() {
        return getMaxBody() - getCurrentBody();
    }

    // === Tick === //

    /**
     * Called every tick to update Body state
     */
    default void tick() {
        // Only cultivate if not at max capacity
        if (canRegenerateBody()) {
            double regenRate = getEffectiveBodyRegenerationRate();

            if (regenRate > 0) {
                // Use the built-in regen fraction methods
                addRegenFraction(regenRate / 20.0); // Convert per-second to per-tick

                if (getRegenFraction() >= 1.0) {
                    long cultivationToAdd = (long) getRegenFraction();
                    setRegenFraction(getRegenFraction() - cultivationToAdd);
                    addBody(cultivationToAdd);
                }
            }
        }
    }

    // === Events === //

    /**
     * Called when Body changes (for syncing, effects, etc.)
     * @param oldBody Previous Body amount
     * @param newBody New Body amount
     */
    default void onBodyChanged(long oldBody, long newBody) {
    }

    /**
     * Called when Body is consumed
     * @param amount Amount consumed
     */
    default void onBodyConsumed(long amount) {
    }

    /**
     * Called when Body is added/regenerated
     * @param amount Amount added
     */
    default void onBodyAdded(long amount) {
    }
}

